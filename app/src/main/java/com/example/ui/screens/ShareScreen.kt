package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.network.PeerDevice
import com.example.network.SelectedFileItem
import com.example.ui.components.DirectConnectDialog
import com.example.ui.components.QrPairingDialog
import com.example.ui.components.RadarScanner
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ShieldEmerald
import com.example.ui.theme.TextMuted
import com.example.util.FileUtils
import com.example.viewmodel.ShareViewModel

@Composable
fun ShareScreen(
  viewModel: ShareViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val networkStatus by viewModel.networkStatus.collectAsStateWithLifecycle()
  val peers by viewModel.peers.collectAsStateWithLifecycle()
  val selectedFiles by viewModel.selectedFiles.collectAsStateWithLifecycle()
  val webShareState by viewModel.webShareState.collectAsStateWithLifecycle()

  var showDirectConnectDialog by remember { mutableStateOf(false) }
  var showQrDialog by remember { mutableStateOf(false) }
  var selectedPeerForSend by remember { mutableStateOf<PeerDevice?>(null) }

  // Safe Android zero-permission Photo Picker
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickMultipleVisualMedia()
  ) { uris ->
    if (uris.isNotEmpty()) {
      viewModel.addSelectedFiles(uris)
    }
  }

  // Document/File Picker
  val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenMultipleDocuments()
  ) { uris ->
    if (uris.isNotEmpty()) {
      viewModel.addSelectedFiles(uris)
    }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Hero Card & Network Banner
    item {
      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 12.dp)
      ) {
        Column {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(130.dp)
          ) {
            Image(
              painter = painterResource(id = R.drawable.hero_e2ee_share_1791876770602),
              contentDescription = "Encrypted WLAN Transfer",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
            // Gradient Overlay
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(
                  Brush.verticalGradient(
                    colors = listOf(Color.Transparent, DarkSurfaceElevated)
                  )
                )
            )

            // Security Badge
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = Color(0xFF0B1938).copy(alpha = 0.9f),
              border = androidx.compose.foundation.BorderStroke(1.dp, ShieldEmerald),
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.Shield,
                  contentDescription = null,
                  tint = ShieldEmerald,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "E2EE AES-256-GCM",
                  style = MaterialTheme.typography.labelSmall,
                  color = ShieldEmerald,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }

          // Network info footer
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = if (networkStatus.isWifiConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                contentDescription = null,
                tint = if (networkStatus.isWifiConnected) CyberCyan else AlertCrimson,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = if (networkStatus.isWifiConnected) networkStatus.ssid else "Wi-Fi Disconnected",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
                Text(
                  text = "IP: ${networkStatus.localIp} • Port 52738",
                  style = MaterialTheme.typography.labelSmall,
                  fontFamily = FontFamily.Monospace,
                  color = TextMuted
                )
              }
            }

            IconButton(onClick = { viewModel.refreshNetwork() }) {
              Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh Wi-Fi", tint = CyberCyan)
            }
          }
        }
      }
    }

    // 2. File Selection Section
    item {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Select Files to Share",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          if (selectedFiles.isNotEmpty()) {
            Text(
              text = "Clear All",
              style = MaterialTheme.typography.labelMedium,
              color = AlertCrimson,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier
                .clickable { viewModel.clearSelectedFiles() }
                .padding(4.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Picker Buttons Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
              )
            },
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("pick_photos_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF162A5A), contentColor = CyberCyan),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
          ) {
            Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Photos/Videos", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
          }

          Button(
            onClick = {
              filePickerLauncher.launch(arrayOf("*/*"))
            },
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("pick_documents_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF162A5A), contentColor = ShieldEmerald),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ShieldEmerald.copy(alpha = 0.5f))
          ) {
            Icon(imageVector = Icons.Default.FilePresent, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Any Document", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
          }
        }
      }
    }

    // Selected Files Display
    if (selectedFiles.isNotEmpty()) {
      item {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = DarkSurface,
          border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            val totalBytes = selectedFiles.sumOf { it.size }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "${selectedFiles.size} file(s) queued",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = CyberCyan
              )
              Text(
                text = "Total: ${FileUtils.formatFileSize(totalBytes)}",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(selectedFiles) { file ->
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = DarkSurfaceElevated,
                  border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(imageVector = Icons.Default.FilePresent, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                      Text(
                        text = file.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.width(100.dp)
                      )
                      Text(
                        text = FileUtils.formatFileSize(file.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 10.sp
                      )
                    }
                    IconButton(
                      onClick = { viewModel.removeSelectedFile(file) },
                      modifier = Modifier.size(20.dp)
                    ) {
                      Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = AlertCrimson, modifier = Modifier.size(14.dp))
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    // 3. Quick Action Hub (Direct Connect, QR Pairing, Web Share)
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Direct Connect
        OutlinedButton(
          onClick = { showDirectConnectDialog = true },
          modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .testTag("direct_connect_action_button"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
          border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
          Icon(imageVector = Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp), tint = CyberCyan)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Direct IP", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        // QR Code Pairing
        OutlinedButton(
          onClick = { showQrDialog = true },
          modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .testTag("qr_pairing_action_button"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
          border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
          Icon(imageVector = Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp), tint = CyberCyan)
          Spacer(modifier = Modifier.width(6.dp))
          Text("My QR", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        // Web Share (PC / Mac)
        Button(
          onClick = { viewModel.toggleWebShare() },
          modifier = Modifier
            .weight(1.2f)
            .height(44.dp)
            .testTag("web_share_toggle_button"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (webShareState.isRunning) ShieldEmerald else Color(0xFF1E2F5E),
            contentColor = Color.White
          )
        ) {
          Icon(imageVector = Icons.Default.Laptop, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(if (webShareState.isRunning) "Web: ON" else "Share to PC", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    // Web Share Active Banner
    if (webShareState.isRunning) {
      item {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Color(0xFF0A261C),
          border = androidx.compose.foundation.BorderStroke(1.dp, ShieldEmerald),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Laptop, contentDescription = null, tint = ShieldEmerald)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Web Share Active on LAN",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = ShieldEmerald
                )
              }
              Text(
                text = "Stop",
                color = AlertCrimson,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { viewModel.toggleWebShare() }
              )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Open on your PC/Mac browser: ${webShareState.url}",
              style = MaterialTheme.typography.bodySmall,
              color = Color.White,
              fontFamily = FontFamily.Monospace
            )
            Text(
              text = "Security PIN: ${webShareState.pin}",
              style = MaterialTheme.typography.labelLarge,
              color = CyberCyan,
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 2.sp
            )
          }
        }
      }
    }

    // 4. Radar Scanner Section
    item {
      Column {
        Text(
          text = "WLAN Radar & Nearby Devices",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
        Text(
          text = "Real-time UDP beacon discovery on local subnet",
          style = MaterialTheme.typography.bodySmall,
          color = TextMuted
        )

        Spacer(modifier = Modifier.height(12.dp))

        RadarScanner(
          peers = peers,
          localDeviceName = viewModel.deviceName,
          onPeerSelected = { peer ->
            if (selectedFiles.isEmpty()) {
              Toast.makeText(context, "Select files above first to send to ${peer.name}", Toast.LENGTH_SHORT).show()
            } else {
              selectedPeerForSend = peer
            }
          }
        )
      }
    }

    // 5. Discovered Peers List
    item {
      Text(
        text = "Available Peers (${peers.size})",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )
    }

    if (peers.isEmpty()) {
      item {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = DarkSurface,
          border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = null,
              tint = CyberCyan.copy(alpha = 0.6f),
              modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Ready to Receive Transfers",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            Text(
              text = "Your device is broadcasting on ${networkStatus.localIp}. Other devices running CipherDrop on the same Wi-Fi will appear automatically, or you can use Direct IP.",
              style = MaterialTheme.typography.bodySmall,
              color = TextMuted,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center,
              modifier = Modifier.padding(top = 4.dp)
            )
          }
        }
      }
    } else {
      items(peers) { peer ->
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
          border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              if (selectedFiles.isEmpty()) {
                Toast.makeText(context, "Select files above first to send to ${peer.name}", Toast.LENGTH_SHORT).show()
              } else {
                selectedPeerForSend = peer
              }
            }
            .testTag("peer_card_${peer.id}")
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(44.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF162550)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.PhoneAndroid,
                  contentDescription = null,
                  tint = CyberCyan,
                  modifier = Modifier.size(24.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = peer.name,
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
                Text(
                  text = "${peer.ip}:${peer.port}",
                  style = MaterialTheme.typography.labelSmall,
                  fontFamily = FontFamily.Monospace,
                  color = TextMuted
                )
              }
            }

            Button(
              onClick = {
                if (selectedFiles.isEmpty()) {
                  Toast.makeText(context, "Select files above first to send to ${peer.name}", Toast.LENGTH_SHORT).show()
                } else {
                  selectedPeerForSend = peer
                }
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedFiles.isNotEmpty()) CyberCyan else DarkSurface,
                contentColor = if (selectedFiles.isNotEmpty()) Color(0xFF00363D) else TextMuted
              ),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(36.dp)
            ) {
              Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Send", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(80.dp))
    }
  }

  // Confirmation Dialog to Send Files
  selectedPeerForSend?.let { peer ->
    AlertDialog(
      onDismissRequest = { selectedPeerForSend = null },
      containerColor = DarkSurface,
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = CyberCyan)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Send Encrypted Files?", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column {
          Text(
            text = "Sending ${selectedFiles.size} file(s) to ${peer.name} (${peer.ip}) over local WLAN.",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "An ephemeral ECDH-P256 key exchange will take place, and every file chunk will be authenticated with AES-256-GCM.",
            color = ShieldEmerald,
            style = MaterialTheme.typography.labelSmall
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.sendFilesToPeer(peer)
            selectedPeerForSend = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color(0xFF00363D)),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("Confirm Send", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { selectedPeerForSend = null },
          colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
          border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("Cancel")
        }
      }
    )
  }

  // QR Pairing Dialog
  if (showQrDialog) {
    val qrPayload = "cipherdrop://${networkStatus.localIp}:52738?id=${viewModel.deviceId}&name=${viewModel.deviceName}"
    QrPairingDialog(
      qrContent = qrPayload,
      title = "Device WLAN Identity",
      subtitle = "Scan with another CipherDrop app or enter IP directly",
      onDismiss = { showQrDialog = false }
    )
  }

  // Direct IP Connect Dialog
  if (showDirectConnectDialog) {
    DirectConnectDialog(
      onConnect = { ip, port ->
        viewModel.connectDirect(ip, port)
      },
      onDismiss = { showDirectConnectDialog = false }
    )
  }
}
