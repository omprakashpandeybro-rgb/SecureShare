package com.example.network

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.crypto.CryptoEngine
import com.example.data.db.AppDatabase
import com.example.data.db.TransferRecord
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.util.UUID

class TransferServer(
  private val context: Context,
  private val serverPort: Int = 52738,
  private val localDeviceName: String,
  private val onIncomingRequest: (IncomingTransferRequest) -> Unit,
  private val onStatusUpdate: (TransferStatus) -> Unit
) {
  companion object {
    private const val TAG = "TransferServer"
  }

  private var serverSocket: ServerSocket? = null
  private var listenJob: Job? = null
  private val database = AppDatabase.getDatabase(context)

  fun start(scope: CoroutineScope) {
    stop()
    listenJob = scope.launch(Dispatchers.IO) {
      try {
        val sSocket = ServerSocket(serverPort).apply {
          reuseAddress = true
        }
        serverSocket = sSocket
        Log.i(TAG, "TransferServer listening on port $serverPort")

        while (isActive && !sSocket.isClosed) {
          try {
            val clientSocket = sSocket.accept()
            launch(Dispatchers.IO) {
              handleClientConnection(clientSocket)
            }
          } catch (e: Exception) {
            if (!isActive) break
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Server error: ${e.message}")
      }
    }
  }

  private suspend fun handleClientConnection(socket: Socket) {
    var recordId: Long = 0
    val startTime = System.currentTimeMillis()
    try {
      socket.soTimeout = 30_000 // 30s timeout for handshake
      val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
      val writer = PrintWriter(socket.getOutputStream(), true)

      // Step 1: Handshake
      val handshakeLine = reader.readLine() ?: throw IllegalStateException("Empty handshake")
      val handshakeJson = JSONObject(handshakeLine)
      val senderName = handshakeJson.optString("senderName", "Unknown Device")
      val clientPublicBase64 = handshakeJson.getString("clientEcdhPublicKey")

      val serverKeyPair = CryptoEngine.generateEcdhKeyPair()
      val serverPublicBase64 = CryptoEngine.encodePublicKey(serverKeyPair.public)

      // Send Server Handshake Response
      val responseJson = JSONObject().apply {
        put("status", "OK")
        put("receiverName", localDeviceName)
        put("serverEcdhPublicKey", serverPublicBase64)
      }
      writer.println(responseJson.toString())

      // Derive shared secret
      val clientPublicKey = CryptoEngine.decodePublicKey(clientPublicBase64)
      val sharedSecretKey = CryptoEngine.deriveSharedSecret(serverKeyPair, clientPublicKey)
      val securityFingerprint = CryptoEngine.computeSecurityFingerprint(sharedSecretKey, clientPublicBase64)

      withContext(Dispatchers.Main) {
        onStatusUpdate(TransferStatus.Handshaking(senderName))
      }

      // Step 2: Metadata
      val metadataLine = reader.readLine() ?: throw IllegalStateException("Empty metadata")
      val metaJson = JSONObject(metadataLine)
      val fileName = metaJson.getString("fileName")
      val fileSize = metaJson.getLong("fileSize")
      val mimeType = metaJson.optString("mimeType", "application/octet-stream")
      val expectedSha256 = metaJson.optString("sha256", "")

      // Prompt user to Accept or Decline
      val acceptDeferred = CompletableDeferred<Boolean>()
      val requestId = UUID.randomUUID().toString()

      val request = IncomingTransferRequest(
        requestId = requestId,
        senderName = senderName,
        senderIp = socket.inetAddress.hostAddress ?: "Unknown",
        fileName = fileName,
        fileSize = fileSize,
        mimeType = mimeType,
        sasCode = securityFingerprint.numericCode,
        emojiSequence = securityFingerprint.emojiSequence,
        onAccept = { acceptDeferred.complete(true) },
        onDecline = { acceptDeferred.complete(false) }
      )

      withContext(Dispatchers.Main) {
        onIncomingRequest(request)
      }

      val accepted = acceptDeferred.await()
      if (!accepted) {
        writer.println(JSONObject().apply { put("status", "DECLINED") }.toString())
        socket.close()
        withContext(Dispatchers.Main) {
          onStatusUpdate(TransferStatus.Idle)
        }
        return
      }

      // Accepted: inform client
      writer.println(JSONObject().apply { put("status", "ACCEPTED") }.toString())

      // Prepare save destination in internal/external app files directory
      val saveDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir, "CipherDrop")
      if (!saveDir.exists()) saveDir.mkdirs()
      var targetFile = File(saveDir, fileName)
      if (targetFile.exists()) {
        val nameWithoutExt = fileName.substringBeforeLast(".")
        val ext = if (fileName.contains(".")) ".${fileName.substringAfterLast(".")}" else ""
        targetFile = File(saveDir, "${nameWithoutExt}_${System.currentTimeMillis() % 10000}$ext")
      }

      val dataIn = DataInputStream(socket.getInputStream())
      val fileOut = FileOutputStream(targetFile)
      val digest = MessageDigest.getInstance("SHA-256")

      var totalBytesReceived = 0L
      var lastReportTime = System.currentTimeMillis()
      var bytesSinceLastReport = 0L

      withContext(Dispatchers.Main) {
        onStatusUpdate(
          TransferStatus.Transferring(
            fileName = fileName,
            fileSize = fileSize,
            bytesTransferred = 0,
            speedMbps = 0.0,
            progressPercent = 0f,
            peerName = senderName,
            isSending = false,
            sasCode = securityFingerprint.numericCode
          )
        )
      }

      // Step 3: Stream encrypted chunks
      while (true) {
        val chunkLen = dataIn.readInt()
        if (chunkLen == 0) {
          // End of file signal
          break
        }
        val encryptedBytes = ByteArray(chunkLen)
        dataIn.readFully(encryptedBytes)

        // Decrypt with AES-GCM
        val decrypted = CryptoEngine.decryptChunk(encryptedBytes, sharedSecretKey)
        fileOut.write(decrypted)
        digest.update(decrypted)

        totalBytesReceived += decrypted.size
        bytesSinceLastReport += decrypted.size

        val now = System.currentTimeMillis()
        if (now - lastReportTime >= 250 || totalBytesReceived >= fileSize) {
          val timeDeltaSec = (now - lastReportTime) / 1000.0
          val speed = if (timeDeltaSec > 0) ((bytesSinceLastReport * 8.0) / (1_000_000.0 * timeDeltaSec)) else 0.0
          val progress = if (fileSize > 0) (totalBytesReceived.toFloat() / fileSize) else 0f

          withContext(Dispatchers.Main) {
            onStatusUpdate(
              TransferStatus.Transferring(
                fileName = fileName,
                fileSize = fileSize,
                bytesTransferred = totalBytesReceived,
                speedMbps = speed,
                progressPercent = progress.coerceIn(0f, 1f),
                peerName = senderName,
                isSending = false,
                sasCode = securityFingerprint.numericCode
              )
            )
          }
          lastReportTime = now
          bytesSinceLastReport = 0L
        }
      }

      fileOut.flush()
      fileOut.close()

      // Step 4: Verification
      val calculatedSha256 = digest.digest().joinToString("") { String.format("%02x", it) }
      val totalDurationMs = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
      val avgSpeedMbps = if (totalDurationMs > 0) ((totalBytesReceived * 8.0) / (1_000_000.0 * (totalDurationMs / 1000.0))) else 0.0

      val verified = expectedSha256.isEmpty() || expectedSha256.equals(calculatedSha256, ignoreCase = true)
      if (verified) {
        val ackJson = JSONObject().apply {
          put("status", "SUCCESS")
          put("verifiedSha256", calculatedSha256)
        }
        writer.println(ackJson.toString())

        // Save to Database
        val record = TransferRecord(
          fileName = fileName,
          fileSize = totalBytesReceived,
          mimeType = mimeType,
          peerName = senderName,
          peerIp = socket.inetAddress.hostAddress ?: "Unknown",
          direction = "RECEIVE",
          status = "COMPLETED",
          transferDurationMs = totalDurationMs,
          avgSpeedMbps = avgSpeedMbps,
          sha256 = calculatedSha256,
          sasCode = securityFingerprint.numericCode,
          localFilePath = targetFile.absolutePath
        )
        database.transferDao().insertTransfer(record)

        withContext(Dispatchers.Main) {
          onStatusUpdate(
            TransferStatus.Completed(
              fileName = fileName,
              fileSize = totalBytesReceived,
              peerName = senderName,
              isSending = false,
              sasCode = securityFingerprint.numericCode,
              sha256 = calculatedSha256,
              durationMs = totalDurationMs,
              localFilePath = targetFile.absolutePath
            )
          )
        }
      } else {
        throw SecurityException("Integrity check failed: Expected $expectedSha256 but got $calculatedSha256")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Transfer reception error: ${e.message}", e)
      withContext(Dispatchers.Main) {
        onStatusUpdate(TransferStatus.Error("Transfer failed: ${e.message}"))
      }
    } finally {
      try {
        socket.close()
      } catch (_: Exception) {}
    }
  }

  fun stop() {
    listenJob?.cancel()
    listenJob = null
    try {
      serverSocket?.close()
      serverSocket = null
    } catch (_: Exception) {}
  }
}
