package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.network.IncomingTransferRequest
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ShieldEmerald
import com.example.ui.theme.TextMuted
import com.example.util.FileUtils
import com.example.util.QrCodeGenerator

@Composable
fun IncomingTransferDialog(
  request: IncomingTransferRequest,
  onAccept: () -> Unit,
  onDecline: () -> Unit
) {
  Dialog(onDismissRequest = onDecline) {
    Surface(
      shape = RoundedCornerShape(28.dp),
      color = DarkSurface,
      border = androidx.compose.foundation.BorderStroke(1.5.dp, ShieldEmerald),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("incoming_transfer_dialog")
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Icon header
        Box(
          modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(Color(0xFF005234))
            .border(2.dp, ShieldEmerald, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Encrypted Transfer",
            tint = ShieldEmerald,
            modifier = Modifier.size(30.dp)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "Incoming Encrypted Transfer",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )

        Text(
          text = "From: ${request.senderName} (${request.senderIp})",
          style = MaterialTheme.typography.bodySmall,
          color = CyberCyan,
          modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // File Card
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = DarkSurfaceElevated,
          border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.FilePresent,
              contentDescription = null,
              tint = CyberCyan,
              modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = request.fileName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
              )
              Text(
                text = FileUtils.formatFileSize(request.fileSize),
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // E2EE SAS Verification Block
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Color(0xFF0B1938),
          border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = CyberCyan,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "E2EE Security Verification Code",
                style = MaterialTheme.typography.labelMedium,
                color = CyberCyan,
                fontWeight = FontWeight.Bold
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = request.sasCode,
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.ExtraBold,
              color = Color.White,
              fontFamily = FontFamily.Monospace,
              letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
              text = request.emojiSequence,
              fontSize = 24.sp,
              modifier = Modifier.padding(vertical = 2.dp)
            )

            Text(
              text = "Confirm this 6-digit code matches the sender's screen to guarantee zero MITM interception.",
              style = MaterialTheme.typography.labelSmall,
              color = TextMuted,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(top = 4.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          OutlinedButton(
            onClick = onDecline,
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("decline_transfer_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertCrimson),
            border = androidx.compose.foundation.BorderStroke(1.dp, AlertCrimson.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(14.dp)
          ) {
            Text("Decline", fontWeight = FontWeight.Bold)
          }

          Button(
            onClick = onAccept,
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("accept_transfer_button"),
            colors = ButtonDefaults.buttonColors(
              containerColor = ShieldEmerald,
              contentColor = Color(0xFF003822)
            ),
            shape = RoundedCornerShape(14.dp)
          ) {
            Text("Accept", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
fun QrPairingDialog(
  qrContent: String,
  title: String,
  subtitle: String,
  onDismiss: () -> Unit
) {
  val qrBitmap = remember(qrContent) {
    QrCodeGenerator.generateQrBitmap(qrContent, sizePx = 400)
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(28.dp),
      color = DarkSurface,
      border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("qr_dialog")
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = CyberCyan)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = title,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
          }
        }

        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = TextMuted,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // QR Bitmap box
        Box(
          modifier = Modifier
            .size(240.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkBackground)
            .border(2.dp, DarkBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
          contentAlignment = Alignment.Center
        ) {
          if (qrBitmap != null) {
            Image(
              bitmap = qrBitmap.asImageBitmap(),
              contentDescription = "QR Code",
              modifier = Modifier.size(208.dp)
            )
          } else {
            Text("Generating QR...", color = TextMuted)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = DarkSurfaceElevated,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = qrContent,
            style = MaterialTheme.typography.labelSmall,
            color = CyberCyan,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(10.dp)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = onDismiss,
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
          colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color(0xFF00363D)),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text("Done", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun DirectConnectDialog(
  onConnect: (ip: String, port: Int) -> Unit,
  onDismiss: () -> Unit
) {
  var ipText by remember { mutableStateOf("192.168.") }
  var portText by remember { mutableStateOf("52738") }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = DarkSurface,
      border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("direct_connect_dialog")
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Wifi, contentDescription = null, tint = CyberCyan)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Direct Connect",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
          }
        }

        Text(
          text = "Enter the target device's IP address on the local WLAN:",
          style = MaterialTheme.typography.bodySmall,
          color = TextMuted,
          modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        OutlinedTextField(
          value = ipText,
          onValueChange = { ipText = it },
          label = { Text("Peer IP Address") },
          placeholder = { Text("e.g. 192.168.1.100") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("direct_ip_input"),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyberCyan,
            unfocusedBorderColor = DarkBorder,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
          ),
          shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = portText,
          onValueChange = { portText = it },
          label = { Text("Port") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyberCyan,
            unfocusedBorderColor = DarkBorder,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
          ),
          shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
          onClick = {
            val port = portText.toIntOrNull() ?: 52738
            if (ipText.isNotBlank()) {
              onConnect(ipText.trim(), port)
              onDismiss()
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("connect_peer_button"),
          colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color(0xFF00363D)),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text("Connect Peer", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
