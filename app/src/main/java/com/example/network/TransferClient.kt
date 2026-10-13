package com.example.network

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.crypto.CryptoEngine
import com.example.data.db.AppDatabase
import com.example.data.db.TransferRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest

class TransferClient(
  private val context: Context,
  private val localDeviceName: String,
  private val onStatusUpdate: (TransferStatus) -> Unit
) {
  companion object {
    private const val TAG = "TransferClient"
    private const val CHUNK_SIZE = 64 * 1024 // 64 KB per AES-GCM chunk
  }

  private val database = AppDatabase.getDatabase(context)

  suspend fun sendFile(
    peer: PeerDevice,
    fileItem: SelectedFileItem
  ) = withContext(Dispatchers.IO) {
    val startTime = System.currentTimeMillis()
    var socket: Socket? = null

    try {
      withContext(Dispatchers.Main) {
        onStatusUpdate(TransferStatus.Connecting(peer.name))
      }

      socket = Socket()
      socket.connect(InetSocketAddress(peer.ip, peer.port), 10_000)
      socket.soTimeout = 45_000

      val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
      val writer = PrintWriter(socket.getOutputStream(), true)

      // Step 1: Handshake
      val clientKeyPair = CryptoEngine.generateEcdhKeyPair()
      val clientPublicBase64 = CryptoEngine.encodePublicKey(clientKeyPair.public)

      val handshakePayload = JSONObject().apply {
        put("version", 1)
        put("senderName", localDeviceName)
        put("clientEcdhPublicKey", clientPublicBase64)
      }
      writer.println(handshakePayload.toString())

      withContext(Dispatchers.Main) {
        onStatusUpdate(TransferStatus.Handshaking(peer.name))
      }

      val serverResponseLine = reader.readLine() ?: throw IllegalStateException("No response from peer")
      val serverResponseJson = JSONObject(serverResponseLine)
      val serverPublicBase64 = serverResponseJson.getString("serverEcdhPublicKey")

      val serverPublicKey = CryptoEngine.decodePublicKey(serverPublicBase64)
      val sharedSecretKey = CryptoEngine.deriveSharedSecret(clientKeyPair, serverPublicKey)
      val securityFingerprint = CryptoEngine.computeSecurityFingerprint(sharedSecretKey, serverPublicBase64)

      // Step 2: Compute initial SHA-256 and prepare metadata
      val shaDigest = MessageDigest.getInstance("SHA-256")
      val inputStreamPre: InputStream? = context.contentResolver.openInputStream(fileItem.uri)
      val bufferPre = ByteArray(CHUNK_SIZE)
      var preRead: Int
      if (inputStreamPre != null) {
        while (inputStreamPre.read(bufferPre).also { preRead = it } != -1) {
          shaDigest.update(bufferPre, 0, preRead)
        }
        inputStreamPre.close()
      }
      val fileSha256 = shaDigest.digest().joinToString("") { String.format("%02x", it) }

      // Step 3: Send Metadata & await acceptance
      val metadataPayload = JSONObject().apply {
        put("fileName", fileItem.name)
        put("fileSize", fileItem.size)
        put("mimeType", fileItem.mimeType)
        put("sha256", fileSha256)
        put("sasCode", securityFingerprint.numericCode)
      }
      writer.println(metadataPayload.toString())

      withContext(Dispatchers.Main) {
        onStatusUpdate(TransferStatus.AwaitingAcceptance(peer.name, securityFingerprint.numericCode))
      }

      val acceptResponseLine = reader.readLine() ?: throw IllegalStateException("Connection dropped while awaiting acceptance")
      val acceptJson = JSONObject(acceptResponseLine)
      val status = acceptJson.getString("status")
      if (status != "ACCEPTED") {
        withContext(Dispatchers.Main) {
          onStatusUpdate(TransferStatus.Error("Transfer declined by recipient (${peer.name})"))
        }
        return@withContext
      }

      // Step 4: Stream Encrypted Chunks
      val dataOut = DataOutputStream(socket.getOutputStream())
      val fileStream: InputStream = context.contentResolver.openInputStream(fileItem.uri)
        ?: throw IllegalArgumentException("Cannot open file stream")

      val chunkBuffer = ByteArray(CHUNK_SIZE)
      var bytesRead: Int
      var totalBytesSent = 0L
      var lastReportTime = System.currentTimeMillis()
      var bytesSinceLastReport = 0L

      withContext(Dispatchers.Main) {
        onStatusUpdate(
          TransferStatus.Transferring(
            fileName = fileItem.name,
            fileSize = fileItem.size,
            bytesTransferred = 0,
            speedMbps = 0.0,
            progressPercent = 0f,
            peerName = peer.name,
            isSending = true,
            sasCode = securityFingerprint.numericCode
          )
        )
      }

      while (fileStream.read(chunkBuffer).also { bytesRead = it } != -1) {
        val encryptedPayload = CryptoEngine.encryptChunk(chunkBuffer, 0, bytesRead, sharedSecretKey)
        // Write chunk length (4 bytes) + encrypted payload
        dataOut.writeInt(encryptedPayload.size)
        dataOut.write(encryptedPayload)

        totalBytesSent += bytesRead
        bytesSinceLastReport += bytesRead

        val now = System.currentTimeMillis()
        if (now - lastReportTime >= 250 || totalBytesSent >= fileItem.size) {
          val timeDeltaSec = (now - lastReportTime) / 1000.0
          val speed = if (timeDeltaSec > 0) ((bytesSinceLastReport * 8.0) / (1_000_000.0 * timeDeltaSec)) else 0.0
          val progress = if (fileItem.size > 0) (totalBytesSent.toFloat() / fileItem.size) else 0f

          withContext(Dispatchers.Main) {
            onStatusUpdate(
              TransferStatus.Transferring(
                fileName = fileItem.name,
                fileSize = fileItem.size,
                bytesTransferred = totalBytesSent,
                speedMbps = speed,
                progressPercent = progress.coerceIn(0f, 1f),
                peerName = peer.name,
                isSending = true,
                sasCode = securityFingerprint.numericCode
              )
            )
          }
          lastReportTime = now
          bytesSinceLastReport = 0L
        }
      }

      fileStream.close()
      // Send EOF terminator
      dataOut.writeInt(0)
      dataOut.flush()

      // Step 5: Await Server ACK
      val finalAckLine = reader.readLine() ?: throw IllegalStateException("No confirmation from receiver")
      val finalAckJson = JSONObject(finalAckLine)
      if (finalAckJson.optString("status") == "SUCCESS") {
        val totalDurationMs = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
        val avgSpeedMbps = if (totalDurationMs > 0) ((totalBytesSent * 8.0) / (1_000_000.0 * (totalDurationMs / 1000.0))) else 0.0

        // Save record to DB
        val record = TransferRecord(
          fileName = fileItem.name,
          fileSize = fileItem.size,
          mimeType = fileItem.mimeType,
          peerName = peer.name,
          peerIp = peer.ip,
          direction = "SEND",
          status = "COMPLETED",
          transferDurationMs = totalDurationMs,
          avgSpeedMbps = avgSpeedMbps,
          sha256 = fileSha256,
          sasCode = securityFingerprint.numericCode
        )
        database.transferDao().insertTransfer(record)

        withContext(Dispatchers.Main) {
          onStatusUpdate(
            TransferStatus.Completed(
              fileName = fileItem.name,
              fileSize = fileItem.size,
              peerName = peer.name,
              isSending = true,
              sasCode = securityFingerprint.numericCode,
              sha256 = fileSha256,
              durationMs = totalDurationMs
            )
          )
        }
      } else {
        throw SecurityException("Receiver reported integrity failure")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Send file error: ${e.message}", e)
      withContext(Dispatchers.Main) {
        onStatusUpdate(TransferStatus.Error("Transfer failed: ${e.message}"))
      }
    } finally {
      try {
        socket?.close()
      } catch (_: Exception) {}
    }
  }
}
