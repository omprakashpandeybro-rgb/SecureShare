package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.network.SelectedFileItem

object FileUtils {

  fun getFileItem(context: Context, uri: Uri): SelectedFileItem {
    var name = "file_${System.currentTimeMillis()}"
    var size = 0L
    var mime = context.contentResolver.getType(uri) ?: "application/octet-stream"

    try {
      context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
          if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
          if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
        }
      }
    } catch (_: Exception) {}

    // Fallback size if query returned 0
    if (size <= 0) {
      try {
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
          size = afd.length
        }
      } catch (_: Exception) {}
    }

    return SelectedFileItem(
      uri = uri,
      name = name,
      size = size,
      mimeType = mime
    )
  }

  fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format("%.2f GB", gb)
  }
}
