package com.example.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class DiscoveryManager(
  private val context: Context,
  private val localDeviceId: String,
  private val localDeviceName: String,
  private val localPort: Int,
  private val localPublicKeyBase64: String
) {
  companion object {
    const val DISCOVERY_PORT = 52737
    private const val TAG = "DiscoveryManager"
  }

  private val _peers = MutableStateFlow<List<PeerDevice>>(emptyList())
  val peers: StateFlow<List<PeerDevice>> = _peers.asStateFlow()

  private var broadcastJob: Job? = null
  private var listenJob: Job? = null
  private var pruneJob: Job? = null
  private var multicastLock: WifiManager.MulticastLock? = null
  private var datagramSocket: DatagramSocket? = null

  fun startDiscovery(scope: CoroutineScope) {
    stopDiscovery()

    multicastLock = NetworkHelper.acquireMulticastLock(context)

    // Start listening for UDP beacons
    listenJob = scope.launch(Dispatchers.IO) {
      try {
        val socket = DatagramSocket(DISCOVERY_PORT).apply {
          broadcast = true
          reuseAddress = true
        }
        datagramSocket = socket
        val buffer = ByteArray(2048)
        val packet = DatagramPacket(buffer, buffer.size)

        while (isActive && !socket.isClosed) {
          try {
            socket.receive(packet)
            val message = String(packet.data, 0, packet.length, Charsets.UTF_8)
            val senderIp = packet.address.hostAddress ?: ""
            handleIncomingBeacon(message, senderIp)
          } catch (e: Exception) {
            if (!isActive) break
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to bind discovery socket: ${e.message}")
      }
    }

    // Start periodic UDP beacon broadcasting
    broadcastJob = scope.launch(Dispatchers.IO) {
      val netStatus = NetworkHelper.getNetworkStatus(context)
      val broadcastAddrStr = netStatus.broadcastAddress

      while (isActive) {
        try {
          val payload = JSONObject().apply {
            put("id", localDeviceId)
            put("name", localDeviceName)
            put("port", localPort)
            put("publicKey", localPublicKeyBase64)
            put("type", "PHONE")
            put("timestamp", System.currentTimeMillis())
          }.toString()

          val bytes = payload.toByteArray(Charsets.UTF_8)
          val broadcastAddr = InetAddress.getByName(broadcastAddrStr)
          val packet = DatagramPacket(bytes, bytes.size, broadcastAddr, DISCOVERY_PORT)

          val sendSocket = DatagramSocket()
          sendSocket.broadcast = true
          sendSocket.send(packet)
          sendSocket.close()

          // Also broadcast to standard 255.255.255.255 if different
          if (broadcastAddrStr != "255.255.255.255") {
            val fallbackPacket = DatagramPacket(bytes, bytes.size, InetAddress.getByName("255.255.255.255"), DISCOVERY_PORT)
            val fbSocket = DatagramSocket()
            fbSocket.broadcast = true
            fbSocket.send(fallbackPacket)
            fbSocket.close()
          }
        } catch (e: Exception) {
          Log.w(TAG, "Broadcast error: ${e.message}")
        }
        delay(2500)
      }
    }

    // Periodic pruning of stale peers (inactive > 8 seconds)
    pruneJob = scope.launch(Dispatchers.Default) {
      while (isActive) {
        delay(4000)
        val now = System.currentTimeMillis()
        val currentList = _peers.value
        val filtered = currentList.filter { now - it.lastSeenTimestamp < 10_000 }
        if (filtered.size != currentList.size) {
          _peers.value = filtered
        }
      }
    }
  }

  private fun handleIncomingBeacon(rawJson: String, senderIp: String) {
    try {
      val obj = JSONObject(rawJson)
      val peerId = obj.getString("id")
      if (peerId == localDeviceId) return // Ignore self-beacons

      val peerName = obj.optString("name", "Unknown Peer")
      val peerPort = obj.optInt("port", localPort)
      val peerKey = obj.optString("publicKey", "")
      val deviceType = obj.optString("type", "PHONE")

      val peer = PeerDevice(
        id = peerId,
        name = peerName,
        ip = senderIp,
        port = peerPort,
        publicKeyBase64 = peerKey,
        lastSeenTimestamp = System.currentTimeMillis(),
        deviceType = deviceType
      )

      val current = _peers.value.toMutableList()
      val idx = current.indexOfFirst { it.id == peerId || (it.ip == senderIp && it.port == peerPort) }
      if (idx >= 0) {
        current[idx] = peer
      } else {
        current.add(peer)
      }
      _peers.value = current
    } catch (_: Exception) {}
  }

  fun addManualPeer(peer: PeerDevice) {
    val current = _peers.value.toMutableList()
    val idx = current.indexOfFirst { it.id == peer.id || it.ip == peer.ip }
    if (idx >= 0) {
      current[idx] = peer
    } else {
      current.add(peer)
    }
    _peers.value = current
  }

  fun stopDiscovery() {
    broadcastJob?.cancel()
    broadcastJob = null
    listenJob?.cancel()
    listenJob = null
    pruneJob?.cancel()
    pruneJob = null
    try {
      datagramSocket?.close()
      datagramSocket = null
    } catch (_: Exception) {}
    try {
      if (multicastLock?.isHeld == true) {
        multicastLock?.release()
      }
      multicastLock = null
    } catch (_: Exception) {}
  }
}
