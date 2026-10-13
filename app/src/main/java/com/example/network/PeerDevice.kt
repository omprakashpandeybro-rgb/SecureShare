package com.example.network

data class PeerDevice(
  val id: String,
  val name: String,
  val ip: String,
  val port: Int = 52738,
  val publicKeyBase64: String,
  val lastSeenTimestamp: Long = System.currentTimeMillis(),
  val deviceType: String = "PHONE"
)
