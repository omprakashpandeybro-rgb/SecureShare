package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transfer_records")
data class TransferRecord(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val fileName: String,
  val fileSize: Long,
  val mimeType: String,
  val peerName: String,
  val peerIp: String,
  val direction: String, // "SEND" or "RECEIVE"
  val status: String,    // "COMPLETED", "FAILED", "CANCELLED"
  val transferDurationMs: Long,
  val avgSpeedMbps: Double,
  val sha256: String,
  val sasCode: String,
  val timestamp: Long = System.currentTimeMillis(),
  val localFilePath: String? = null
)
