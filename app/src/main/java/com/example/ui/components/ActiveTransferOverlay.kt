package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.network.TransferStatus
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ShieldEmerald
import com.example.ui.theme.TextMuted
import com.example.util.FileUtils
import java.io.File

@Composable
fun ActiveTransferOverlay(
  transferStatus: TransferStatus,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  AnimatedVisibility(
    visible = transferStatus !is TransferStatus.Idle,
    enter = slideInVertically { it } + fadeIn(),
    exit = slideOutVertically { it } + fadeOut(),
    modifier = modifier
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = DarkSurface,
      border = androidx.compose.foundation.BorderStroke(1.5.dp, CyberCyan.copy(alpha = 0.8f)),
      shadowElevation = 16.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("active_transfer_overlay")
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        when (transferStatus) {
          is TransferStatus.Connecting -> {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              CircularProgressIndicator(
                color = CyberCyan,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
              )
              Spacer(modifier = Modifier.width(16.dp))
              Column {
                Text(
                  text = "Establishing Direct WLAN Socket",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
                Text(
                  text = "Connecting to ${transferStatus.peerName}...",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextMuted
                )
              }
            }
          }

          is TransferStatus.Handshaking -> {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              CircularProgressIndicator(
                color = ShieldEmerald,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
              )
              Spacer(modifier = Modifier.width(16.dp))
              Column {
                Text(
                  text = "ECDH-P256 Key Exchange",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
                Text(
                  text = "Deriving 256-bit AES-GCM session key...",
                  style = MaterialTheme.typography.bodySmall,
                  color = ShieldEmerald
                )
              }
            }
          }

          is TransferStatus.AwaitingAcceptance -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              CircularProgressIndicator(color = CyberCyan, modifier = Modifier.size(32.dp))
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "Waiting for ${transferStatus.peerName} to accept...",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
              Spacer(modifier = Modifier.height(8.dp))
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkSurfaceElevated,
                modifier = Modifier.padding(vertical = 4.dp)
              ) {
                Text(
                  text = "Security Match Code: ${transferStatus.sasCode}",
                  style = MaterialTheme.typography.labelMedium,
                  color = CyberCyan,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
              }
            }
          }

          is TransferStatus.Transferring -> {
            Column(modifier = Modifier.fillMaxWidth()) {
              // Header
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = if (transferStatus.isSending) Icons.Default.Upload else Icons.Default.Download,
                    contentDescription = null,
                    tint = if (transferStatus.isSending) CyberCyan else ShieldEmerald,
                    modifier = Modifier.size(24.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text(
                      text = if (transferStatus.isSending) "Sending Encrypted File" else "Receiving Encrypted File",
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      color = Color.White
                    )
                    Text(
                      text = transferStatus.fileName,
                      style = MaterialTheme.typography.bodySmall,
                      color = TextMuted,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                  }
                }

                // Speed Pill
                Surface(
                  shape = RoundedCornerShape(999.dp),
                  color = DarkSurfaceElevated,
                  border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
                ) {
                  Text(
                    text = String.format("%.1f MB/s", transferStatus.speedMbps),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = CyberCyan,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(14.dp))

              // Linear Progress Indicator
              LinearProgressIndicator(
                progress = { transferStatus.progressPercent },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(10.dp)
                  .clip(RoundedCornerShape(5.dp)),
                color = if (transferStatus.isSending) CyberCyan else ShieldEmerald,
                trackColor = Color(0xFF1E2D5A)
              )

              Spacer(modifier = Modifier.height(8.dp))

              // Byte count & percentage
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "${FileUtils.formatFileSize(transferStatus.bytesTransferred)} / ${FileUtils.formatFileSize(transferStatus.fileSize)}",
                  style = MaterialTheme.typography.labelSmall,
                  color = TextMuted
                )
                Text(
                  text = "${(transferStatus.progressPercent * 100).toInt()}%",
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
              }

              Spacer(modifier = Modifier.height(12.dp))

              // Security Chip
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(Color(0xFF0D1B3E), RoundedCornerShape(10.dp))
                  .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = ShieldEmerald,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = transferStatus.cipherSuite,
                    style = MaterialTheme.typography.labelSmall,
                    color = ShieldEmerald,
                    fontSize = 11.sp
                  )
                }
                Text(
                  text = "SAS: ${transferStatus.sasCode}",
                  style = MaterialTheme.typography.labelSmall,
                  color = CyberCyan,
                  fontFamily = FontFamily.Monospace,
                  fontSize = 11.sp
                )
              }
            }
          }

          is TransferStatus.Completed -> {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.fillMaxWidth()
            ) {
              Box(
                modifier = Modifier
                  .size(54.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF005234)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = ShieldEmerald,
                  modifier = Modifier.size(34.dp)
                )
              }

              Spacer(modifier = Modifier.height(10.dp))

              Text(
                text = "Transfer Verified & Complete!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )

              Text(
                text = "${transferStatus.fileName} (${FileUtils.formatFileSize(transferStatus.fileSize)})",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )

              Spacer(modifier = Modifier.height(10.dp))

              // Hash verification badge
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = DarkSurfaceElevated,
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Text(
                    text = "SHA-256 Bit-for-Bit Hash:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 10.sp
                  )
                  Text(
                    text = transferStatus.sha256.take(24) + "...",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                  )
                }
              }

              Spacer(modifier = Modifier.height(16.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                if (transferStatus.localFilePath != null) {
                  Button(
                    onClick = {
                      openReceivedFile(context, transferStatus.localFilePath)
                    },
                    modifier = Modifier
                      .weight(1f)
                      .height(46.dp)
                      .testTag("open_received_file_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color(0xFF00363D)),
                    shape = RoundedCornerShape(12.dp)
                  ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open File", fontWeight = FontWeight.Bold)
                  }
                }

                Button(
                  onClick = onDismiss,
                  modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("dismiss_transfer_button"),
                  colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated, contentColor = Color.White),
                  shape = RoundedCornerShape(12.dp)
                ) {
                  Text("Done", fontWeight = FontWeight.SemiBold)
                }
              }
            }
          }

          is TransferStatus.Error -> {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.fillMaxWidth()
            ) {
              Box(
                modifier = Modifier
                  .size(54.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF450A0A)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.ErrorOutline,
                  contentDescription = null,
                  tint = AlertCrimson,
                  modifier = Modifier.size(34.dp)
                )
              }

              Spacer(modifier = Modifier.height(10.dp))

              Text(
                text = "Transfer Failed",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
              )

              Text(
                text = transferStatus.message,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                modifier = Modifier.padding(top = 4.dp)
              )

              Spacer(modifier = Modifier.height(16.dp))

              Button(
                onClick = onDismiss,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
              ) {
                Text("Dismiss", fontWeight = FontWeight.SemiBold)
              }
            }
          }

          else -> {}
        }
      }
    }
  }
}

private fun openReceivedFile(context: Context, filePath: String) {
  try {
    val file = File(filePath)
    if (!file.exists()) {
      Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show()
      return
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val mimeType = context.contentResolver.getType(uri) ?: "*/*"
    val intent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(uri, mimeType)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(intent, "Open with"))
  } catch (e: Exception) {
    Toast.makeText(context, "Could not open file: ${e.message}", Toast.LENGTH_SHORT).show()
  }
}
