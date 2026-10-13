package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections

object NetworkHelper {

  data class NetworkStatus(
    val isWifiConnected: Boolean,
    val localIp: String,
    val broadcastAddress: String,
    val interfaceName: String,
    val ssid: String
  )

  fun getNetworkStatus(context: Context): NetworkStatus {
    var isWifi = false
    val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    if (connManager != null) {
      val activeNet = connManager.activeNetwork
      val caps = connManager.getNetworkCapabilities(activeNet)
      isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }

    var localIp = "127.0.0.1"
    var broadcast = "255.255.255.255"
    var ifName = "wlan0"

    try {
      val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
      for (intf in interfaces) {
        if (intf.isLoopback || !intf.isUp) continue
        val addresses = Collections.list(intf.inetAddresses)
        for (addr in addresses) {
          if (!addr.isLoopbackAddress && addr is Inet4Address) {
            val hostAddr = addr.hostAddress ?: continue
            // Prefer wlan or typical private IP ranges
            if (intf.name.contains("wlan") || intf.name.contains("ap") || hostAddr.startsWith("192.168.") || hostAddr.startsWith("10.") || hostAddr.startsWith("172.")) {
              localIp = hostAddr
              ifName = intf.name
              // Derive broadcast address from InterfaceAddress
              for (ifAddr in intf.interfaceAddresses) {
                if (ifAddr.address is Inet4Address && ifAddr.broadcast != null) {
                  broadcast = ifAddr.broadcast.hostAddress ?: "255.255.255.255"
                  break
                }
              }
              break
            }
          }
        }
        if (localIp != "127.0.0.1") break
      }
    } catch (_: Exception) {
      // Fallback
    }

    var ssidName = "Local Wi-Fi"
    try {
      val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
      val info = wifiManager?.connectionInfo
      if (info != null && info.ssid != null && info.ssid != "<unknown ssid>") {
        ssidName = info.ssid.replace("\"", "")
      }
    } catch (_: Exception) {}

    return NetworkStatus(
      isWifiConnected = isWifi || localIp != "127.0.0.1",
      localIp = localIp,
      broadcastAddress = broadcast,
      interfaceName = ifName,
      ssid = ssidName
    )
  }

  fun acquireMulticastLock(context: Context): WifiManager.MulticastLock? {
    return try {
      val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
      val lock = wifi?.createMulticastLock("CipherDropMulticast")
      lock?.setReferenceCounted(true)
      lock?.acquire()
      lock
    } catch (_: Exception) {
      null
    }
  }
}
