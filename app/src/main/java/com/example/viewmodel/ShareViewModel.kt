package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.BenchmarkResult
import com.example.crypto.CryptoEngine
import com.example.crypto.SecurityFingerprint
import com.example.data.db.AppDatabase
import com.example.data.db.TransferRecord
import com.example.network.DiscoveryManager
import com.example.network.IncomingTransferRequest
import com.example.network.NetworkHelper
import com.example.network.PeerDevice
import com.example.network.SelectedFileItem
import com.example.network.TransferClient
import com.example.network.TransferServer
import com.example.network.TransferStatus
import com.example.network.WebShareServer
import com.example.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class WebShareState(
  val isRunning: Boolean = false,
  val url: String = "",
  val pin: String = "",
  val port: Int = 8080
)

class ShareViewModel(application: Application) : AndroidViewModel(application) {

  private val context: Context get() = getApplication<Application>().applicationContext
  private val database = AppDatabase.getDatabase(context)

  // Local Device Identity
  val deviceId: String = UUID.randomUUID().toString().substring(0, 8)
  val deviceName: String = run {
    val model = Build.MODEL ?: "Android Device"
    val manufacturer = Build.MANUFACTURER ?: ""
    if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
  }

  // Persistent Device Cryptographic Identity for Verification
  private val permanentKeyPair = CryptoEngine.generateEcdhKeyPair()
  val publicIdentityBase64: String = CryptoEngine.encodePublicKey(permanentKeyPair.public)
  val identityFingerprint: SecurityFingerprint = CryptoEngine.computeSecurityFingerprint(
    CryptoEngine.deriveSharedSecret(permanentKeyPair, permanentKeyPair.public),
    publicIdentityBase64
  )

  // Network Status
  private val _networkStatus = MutableStateFlow(NetworkHelper.getNetworkStatus(context))
  val networkStatus: StateFlow<NetworkHelper.NetworkStatus> = _networkStatus.asStateFlow()

  // Selected Files to send
  private val _selectedFiles = MutableStateFlow<List<SelectedFileItem>>(emptyList())
  val selectedFiles: StateFlow<List<SelectedFileItem>> = _selectedFiles.asStateFlow()

  // Active Transfer Status
  private val _transferStatus = MutableStateFlow<TransferStatus>(TransferStatus.Idle)
  val transferStatus: StateFlow<TransferStatus> = _transferStatus.asStateFlow()

  // Incoming Request Dialog State
  private val _incomingRequest = MutableStateFlow<IncomingTransferRequest?>(null)
  val incomingRequest: StateFlow<IncomingTransferRequest?> = _incomingRequest.asStateFlow()

  // Web Share Mode
  private val _webShareState = MutableStateFlow(WebShareState())
  val webShareState: StateFlow<WebShareState> = _webShareState.asStateFlow()

  // Hardware Benchmark
  private val _benchmarkResult = MutableStateFlow<BenchmarkResult?>(null)
  val benchmarkResult: StateFlow<BenchmarkResult?> = _benchmarkResult.asStateFlow()
  private val _isBenchmarking = MutableStateFlow(false)
  val isBenchmarking: StateFlow<Boolean> = _isBenchmarking.asStateFlow()

  // Transfer History from Room
  val transferHistory: StateFlow<List<TransferRecord>> = database.transferDao()
    .getAllTransfers()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Network Managers
  private val discoveryManager = DiscoveryManager(
    context = context,
    localDeviceId = deviceId,
    localDeviceName = deviceName,
    localPort = 52738,
    localPublicKeyBase64 = publicIdentityBase64
  )

  val peers: StateFlow<List<PeerDevice>> = discoveryManager.peers

  private val transferServer = TransferServer(
    context = context,
    serverPort = 52738,
    localDeviceName = deviceName,
    onIncomingRequest = { req ->
      _incomingRequest.value = req
    },
    onStatusUpdate = { status ->
      _transferStatus.value = status
      if (status is TransferStatus.Completed || status is TransferStatus.Error) {
        _incomingRequest.value = null
      }
    }
  )

  private val transferClient = TransferClient(
    context = context,
    localDeviceName = deviceName,
    onStatusUpdate = { status ->
      _transferStatus.value = status
    }
  )

  private val webShareServer = WebShareServer(
    context = context,
    port = 8080,
    onFileUploaded = { _ ->
      // Handled
    }
  )

  init {
    startServices()
  }

  fun startServices() {
    refreshNetwork()
    discoveryManager.startDiscovery(viewModelScope)
    transferServer.start(viewModelScope)
  }

  fun refreshNetwork() {
    _networkStatus.value = NetworkHelper.getNetworkStatus(context)
  }

  fun addSelectedFiles(uris: List<Uri>) {
    val current = _selectedFiles.value.toMutableList()
    for (uri in uris) {
      val item = FileUtils.getFileItem(context, uri)
      if (current.none { it.uri == uri }) {
        current.add(item)
      }
    }
    _selectedFiles.value = current
  }

  fun removeSelectedFile(item: SelectedFileItem) {
    _selectedFiles.value = _selectedFiles.value.filter { it.uri != item.uri }
  }

  fun clearSelectedFiles() {
    _selectedFiles.value = emptyList()
  }

  fun sendFilesToPeer(peer: PeerDevice) {
    val files = _selectedFiles.value
    if (files.isEmpty()) return

    viewModelScope.launch(Dispatchers.IO) {
      // Send selected files sequentially
      for (file in files) {
        transferClient.sendFile(peer, file)
        // Check if error occurred
        if (_transferStatus.value is TransferStatus.Error) {
          break
        }
      }
    }
  }

  fun connectDirect(ip: String, port: Int = 52738, name: String = "Direct Peer") {
    val manualPeer = PeerDevice(
      id = "direct-$ip",
      name = name,
      ip = ip,
      port = port,
      publicKeyBase64 = "",
      deviceType = "MANUAL"
    )
    discoveryManager.addManualPeer(manualPeer)
  }

  fun acceptIncomingRequest() {
    _incomingRequest.value?.let { req ->
      req.onAccept()
      _incomingRequest.value = null
    }
  }

  fun declineIncomingRequest() {
    _incomingRequest.value?.let { req ->
      req.onDecline()
      _incomingRequest.value = null
      _transferStatus.value = TransferStatus.Idle
    }
  }

  fun dismissTransferStatus() {
    _transferStatus.value = TransferStatus.Idle
  }

  fun toggleWebShare() {
    if (_webShareState.value.isRunning) {
      webShareServer.stop()
      _webShareState.value = WebShareState(isRunning = false)
    } else {
      val file = _selectedFiles.value.firstOrNull()
      val pin = webShareServer.start(viewModelScope, file)
      val localIp = _networkStatus.value.localIp
      val url = "http://$localIp:8080"
      _webShareState.value = WebShareState(
        isRunning = true,
        url = url,
        pin = pin,
        port = 8080
      )
    }
  }

  fun runHardwareBenchmark() {
    viewModelScope.launch(Dispatchers.Default) {
      _isBenchmarking.value = true
      val res = CryptoEngine.runBenchmark()
      _benchmarkResult.value = res
      _isBenchmarking.value = false
    }
  }

  fun deleteHistory(record: TransferRecord) {
    viewModelScope.launch(Dispatchers.IO) {
      database.transferDao().deleteTransfer(record)
    }
  }

  fun clearAllHistory() {
    viewModelScope.launch(Dispatchers.IO) {
      database.transferDao().clearAll()
    }
  }

  override fun onCleared() {
    super.onCleared()
    discoveryManager.stopDiscovery()
    transferServer.stop()
    webShareServer.stop()
  }
}
