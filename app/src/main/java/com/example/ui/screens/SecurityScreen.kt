package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ShieldEmerald
import com.example.ui.theme.TextMuted
import com.example.viewmodel.ShareViewModel

@Composable
fun SecurityScreen(
  viewModel: ShareViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val networkStatus by viewModel.networkStatus.collectAsStateWithLifecycle()
  val benchmarkResult by viewModel.benchmarkResult.collectAsStateWithLifecycle()
  val isBenchmarking by viewModel.isBenchmarking.collectAsStateWithLifecycle()

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Header
    item {
      Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(
          text = "Security & Encryption Hub",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
        Text(
          text = "Military-grade End-to-End Cryptographic Specifications",
          style = MaterialTheme.typography.bodySmall,
          color = TextMuted
        )
      }
    }

    // 2. Device Cryptographic Identity
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF003848)),
                contentAlignment = Alignment.Center
              ) {
                Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(22.dp))
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Permanent Device Key",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
                Text(
                  text = "ECDH SECP256R1 (NIST P-256)",
                  style = MaterialTheme.typography.labelSmall,
                  color = ShieldEmerald
                )
              }
            }

            IconButton(onClick = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              clipboard.setPrimaryClip(ClipData.newPlainText("Public Key", viewModel.publicIdentityBase64))
              Toast.makeText(context, "Public key copied to clipboard", Toast.LENGTH_SHORT).show()
            }) {
              Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Key", tint = CyberCyan)
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          Surface(
            shape = RoundedCornerShape(12.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = "Cryptographic Fingerprint:",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
              )
              Text(
                text = viewModel.identityFingerprint.hexFingerprint,
                style = MaterialTheme.typography.labelMedium,
                color = CyberCyan,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Emoji SAS: ${viewModel.identityFingerprint.emojiSequence}",
                fontSize = 18.sp
              )
            }
          }
        }
      }
    }

    // 3. Hardware Encryption Speed Test (Benchmark)
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF004D25)),
                contentAlignment = Alignment.Center
              ) {
                Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = ShieldEmerald, modifier = Modifier.size(22.dp))
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Hardware Encryption Speed",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = Color.White
                )
                Text(
                  text = "On-Device AES-256-GCM AEAD Throughput",
                  style = MaterialTheme.typography.labelSmall,
                  color = TextMuted
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          if (benchmarkResult != null) {
            val res = benchmarkResult!!
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0xFF0D1B3E),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(
                    text = String.format("%.1f MB/s", res.throughputMbps),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = CyberCyan
                  )
                  Text(
                    text = "Tested 4MB Payload in ${String.format("%.1f", res.timeMs)} ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                  )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = ShieldEmerald, modifier = Modifier.size(20.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Hardware Accelerated", color = ShieldEmerald, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
              }
            }
            Spacer(modifier = Modifier.height(12.dp))
          }

          Button(
            onClick = { viewModel.runHardwareBenchmark() },
            enabled = !isBenchmarking,
            modifier = Modifier
              .fillMaxWidth()
              .height(46.dp)
              .testTag("run_benchmark_button"),
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color(0xFF00363D)),
            shape = RoundedCornerShape(12.dp)
          ) {
            if (isBenchmarking) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF00363D), strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Benchmarking Cipher...", fontWeight = FontWeight.Bold)
            } else {
              Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(if (benchmarkResult == null) "Run Hardware Speed Test" else "Re-test Throughput", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // 4. Zero-Cloud E2EE Guarantees & Architecture
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Text(
            text = "E2EE Security Protocol Breakdown",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          Spacer(modifier = Modifier.height(12.dp))

          val specs = listOf(
            Triple("Key Agreement", "ECDH on SECP256R1 (P-256)", "Ephemeral keys generated per session, never saved to disk or reused."),
            Triple("Data Encryption", "AES-256 in GCM Mode (AEAD)", "Authenticated 128-bit tags. Any byte tampering in transit causes instant rejection."),
            Triple("MITM Protection", "6-Digit SAS + Emoji Fingerprint", "Users verify matching security code to ensure no hostile router is intercepting."),
            Triple("Integrity Check", "SHA-256 Bit-for-Bit Hash", "Computes pre- and post-transfer cryptographic digest to guarantee zero corruption."),
            Triple("Network Transport", "Direct WLAN Raw TCP Sockets", "100% peer-to-peer on local subnet. Zero external cloud relays or servers.")
          )

          specs.forEach { (title, subtitle, desc) ->
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(ShieldEmerald)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "• $subtitle", style = MaterialTheme.typography.labelSmall, color = CyberCyan)
              }
              Text(text = desc, style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.padding(start = 16.dp, top = 2.dp))
            }
          }
        }
      }
    }

    // 5. WLAN Interface Diagnostics
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.NetworkCheck, contentDescription = null, tint = CyberCyan)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Local WLAN Diagnostics",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          listOf(
            "Local IPv4 Address" to networkStatus.localIp,
            "Subnet Broadcast IP" to networkStatus.broadcastAddress,
            "Network Interface" to networkStatus.interfaceName,
            "Transfer Port" to "52738 (TCP)",
            "Discovery Port" to "52737 (UDP)"
          ).forEach { (label, value) ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
              Text(text = value, style = MaterialTheme.typography.labelSmall, color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(80.dp))
    }
  }
}
