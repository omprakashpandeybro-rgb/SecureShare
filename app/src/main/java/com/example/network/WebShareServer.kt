package com.example.network

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.db.TransferRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import kotlin.random.Random

class WebShareServer(
  private val context: Context,
  private val port: Int = 8080,
  private val onFileUploaded: (String) -> Unit
) {
  companion object {
    private const val TAG = "WebShareServer"
  }

  private var serverSocket: ServerSocket? = null
  private var serverJob: Job? = null
  private var sharedFile: SelectedFileItem? = null
  private var securityPin: String = String.format("%04d", Random.nextInt(1000, 9999))
  private val database = AppDatabase.getDatabase(context)

  fun start(scope: CoroutineScope, fileToShare: SelectedFileItem?): String {
    stop()
    sharedFile = fileToShare
    securityPin = String.format("%04d", Random.nextInt(1000, 9999))

    serverJob = scope.launch(Dispatchers.IO) {
      try {
        val sSocket = ServerSocket(port).apply { reuseAddress = true }
        serverSocket = sSocket
        Log.i(TAG, "WebShareServer running on port $port, PIN: $securityPin")

        while (isActive && !sSocket.isClosed) {
          try {
            val client = sSocket.accept()
            launch(Dispatchers.IO) {
              handleHttpClient(client)
            }
          } catch (e: Exception) {
            if (!isActive) break
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Web server error: ${e.message}")
      }
    }

    return securityPin
  }

  private fun handleHttpClient(socket: Socket) {
    try {
      socket.soTimeout = 15_000
      val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
      val out = socket.getOutputStream()

      val requestLine = reader.readLine() ?: return
      val parts = requestLine.split(" ")
      if (parts.size < 2) return

      val method = parts[0]
      val rawPath = parts[1]
      val url = URLDecoder.decode(rawPath, "UTF-8")

      // Read headers
      var headerLine: String?
      var contentLength = 0
      var contentType = ""
      while (reader.readLine().also { headerLine = it } != null) {
        if (headerLine.isNullOrEmpty()) break
        val lower = headerLine!!.lowercase()
        if (lower.startsWith("content-length:")) {
          contentLength = headerLine!!.substringAfter(":").trim().toIntOrNull() ?: 0
        } else if (lower.startsWith("content-type:")) {
          contentType = headerLine!!.substringAfter(":").trim()
        }
      }

      when {
        url == "/" || url.startsWith("/?") -> {
          sendHtmlPage(out)
        }
        url.startsWith("/download") -> {
          val queryPin = url.substringAfter("pin=", "")
          if (queryPin == securityPin && sharedFile != null) {
            sendFileDownload(out, sharedFile!!)
          } else {
            sendError(out, 403, "Invalid Security PIN")
          }
        }
        method == "POST" && url.startsWith("/upload") -> {
          // Accept uploaded bytes
          handleFileUpload(socket.getInputStream(), contentLength, contentType, out)
        }
        else -> {
          sendError(out, 404, "Not Found")
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "HTTP client error: ${e.message}")
    } finally {
      try { socket.close() } catch (_: Exception) {}
    }
  }

  private fun sendHtmlPage(out: OutputStream) {
    val file = sharedFile
    val fileSection = if (file != null) {
      """
      <div class="card">
        <div class="file-icon">📁</div>
        <div class="file-name">${file.name}</div>
        <div class="file-size">${formatBytes(file.size)}</div>
        <p style="color:#94a3b8; font-size:14px; margin-top:8px;">Enter the 4-digit security PIN shown on the phone screen to download:</p>
        <div class="pin-group">
          <input type="text" id="pinInput" placeholder="Enter PIN" maxlength="4" />
          <button onclick="downloadFile()">Download File</button>
        </div>
      </div>
      """
    } else {
      """
      <div class="card">
        <p style="color:#94a3b8;">No file is currently selected on the sender device.</p>
      </div>
      """
    }

    val html = """
      <!DOCTYPE html>
      <html lang="en">
      <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>CipherDrop Web Share</title>
        <style>
          * { box-sizing: border-box; margin:0; padding:0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
          body { background: #0b132b; color: #f1f5f9; display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 100vh; padding: 20px; }
          .container { width: 100%; max-width: 480px; text-align: center; }
          .logo { font-size: 28px; font-weight: 800; color: #00e5ff; letter-spacing: -0.5px; margin-bottom: 4px; }
          .badge { display: inline-block; background: #005234; color: #10b981; font-size: 12px; font-weight: 600; padding: 4px 10px; border-radius: 999px; margin-bottom: 24px; }
          .card { background: #111d3e; border: 1px solid #2c3e75; border-radius: 16px; padding: 24px; margin-bottom: 20px; box-shadow: 0 10px 25px rgba(0,0,0,0.5); }
          .file-icon { font-size: 48px; margin-bottom: 12px; }
          .file-name { font-size: 18px; font-weight: 700; word-break: break-all; margin-bottom: 6px; }
          .file-size { font-size: 14px; color: #00e5ff; font-weight: 600; }
          .pin-group { display: flex; gap: 10px; margin-top: 16px; justify-content: center; }
          input { background: #0b132b; border: 1px solid #2c3e75; color: #f1f5f9; padding: 12px; font-size: 18px; border-radius: 10px; width: 130px; text-align: center; letter-spacing: 4px; font-weight: 700; }
          button { background: #00e5ff; color: #0b132b; border: none; font-weight: 700; padding: 12px 20px; border-radius: 10px; cursor: pointer; transition: opacity 0.2s; }
          button:hover { opacity: 0.9; }
          .footer { font-size: 12px; color: #64748b; margin-top: 20px; }
        </style>
      </head>
      <body>
        <div class="container">
          <div class="logo">⚡ CipherDrop</div>
          <div class="badge">🔒 Direct WLAN Local Transfer</div>
          $fileSection
          <div class="footer">Zero Cloud • Direct Peer-to-Peer Wi-Fi Stream</div>
        </div>
        <script>
          function downloadFile() {
            var pin = document.getElementById('pinInput').value.trim();
            if (!pin) { alert('Please enter PIN'); return; }
            window.location.href = '/download?pin=' + encodeURIComponent(pin);
          }
        </script>
      </body>
      </html>
    """.trimIndent()

    val response = "HTTP/1.1 200 OK\r\n" +
      "Content-Type: text/html; charset=utf-8\r\n" +
      "Content-Length: ${html.toByteArray(Charsets.UTF_8).size}\r\n" +
      "Connection: close\r\n\r\n" +
      html

    out.write(response.toByteArray(Charsets.UTF_8))
    out.flush()
  }

  private fun sendFileDownload(out: OutputStream, fileItem: SelectedFileItem) {
    try {
      val input: InputStream? = context.contentResolver.openInputStream(fileItem.uri)
      if (input == null) {
        sendError(out, 500, "Could not open file")
        return
      }

      val header = "HTTP/1.1 200 OK\r\n" +
        "Content-Type: ${fileItem.mimeType.ifEmpty { "application/octet-stream" }}\r\n" +
        "Content-Length: ${fileItem.size}\r\n" +
        "Content-Disposition: attachment; filename=\"${fileItem.name}\"\r\n" +
        "Connection: close\r\n\r\n"

      out.write(header.toByteArray(Charsets.UTF_8))

      val buffer = ByteArray(64 * 1024)
      var read: Int
      while (input.read(buffer).also { read = it } != -1) {
        out.write(buffer, 0, read)
      }
      out.flush()
      input.close()
    } catch (e: Exception) {
      Log.e(TAG, "File download error: ${e.message}")
    }
  }

  private fun handleFileUpload(input: InputStream, length: Int, contentType: String, out: OutputStream) {
    try {
      val saveDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "WebUploads")
      if (!saveDir.exists()) saveDir.mkdirs()
      val target = File(saveDir, "upload_${System.currentTimeMillis()}.bin")
      val fileOut = FileOutputStream(target)

      val buffer = ByteArray(32 * 1024)
      var total = 0
      while (total < length) {
        val toRead = Math.min(buffer.size, length - total)
        val read = input.read(buffer, 0, toRead)
        if (read == -1) break
        fileOut.write(buffer, 0, read)
        total += read
      }
      fileOut.close()
      onFileUploaded(target.name)

      val response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nConnection: close\r\n\r\nOK"
      out.write(response.toByteArray())
      out.flush()
    } catch (_: Exception) {
      sendError(out, 500, "Upload error")
    }
  }

  private fun sendError(out: OutputStream, code: Int, message: String) {
    val body = "<h1>$code $message</h1>"
    val res = "HTTP/1.1 $code $message\r\nContent-Type: text/html\r\nContent-Length: ${body.length}\r\nConnection: close\r\n\r\n$body"
    out.write(res.toByteArray())
    out.flush()
  }

  private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format("%.2f GB", gb)
  }

  fun stop() {
    serverJob?.cancel()
    serverJob = null
    try {
      serverSocket?.close()
      serverSocket = null
    } catch (_: Exception) {}
  }
}
