package com.example.network

import android.net.Uri

sealed class TransferStatus {
  object Idle : TransferStatus()
  data class Connecting(val peerName: String) : TransferStatus()
  data class Handshaking(val peerName: String) : TransferStatus()
  data class AwaitingAcceptance(val peerName: String, val sasCode: String) : TransferStatus()
  data class Transferring(
    val fileName: String,
    val fileSize: Long,
    val bytesTransferred: Long,
    val speedMbps: Double,
    val progressPercent: Float,
    val peerName: String,
    val isSending: Boolean,
    val sasCode: String,
    val cipherSuite: String = "AES-256-GCM • ECDH-P256"
  ) : TransferStatus()
  data class Completed(
    val fileName: String,
    val fileSize: Long,
    val peerName: String,
    val isSending: Boolean,
    val sasCode: String,
    val sha256: String,
    val durationMs: Long,
    val localFilePath: String? = null
  ) : TransferStatus()
  data class Error(val message: String) : TransferStatus()
}

data class SelectedFileItem(
  val uri: Uri,
  val name: String,
  val size: Long,
  val mimeType: String
)

data class IncomingTransferRequest(
  val requestId: String,
  val senderName: String,
  val senderIp: String,
  val fileName: String,
  val fileSize: Long,
  val mimeType: String,
  val sasCode: String,
  val emojiSequence: String,
  val onAccept: () -> Unit,
  val onDecline: () -> Unit
)
