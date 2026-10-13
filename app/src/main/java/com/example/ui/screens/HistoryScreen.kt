package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.TransferRecord
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ShieldEmerald
import com.example.ui.theme.TextMuted
import com.example.util.FileUtils
import com.example.viewmodel.ShareViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
  viewModel: ShareViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val historyList by viewModel.transferHistory.collectAsStateWithLifecycle()
  var filterDirection by remember { mutableStateOf("ALL") } // "ALL", "RECEIVE", "SEND"
  var showClearConfirm by remember { mutableStateOf(false) }

  val filteredList = remember(historyList, filterDirection) {
    when (filterDirection) {
      "RECEIVE" -> historyList.filter { it.direction == "RECEIVE" }
      "SEND" -> historyList.filter { it.direction == "SEND" }
      else -> historyList
    }
  }

  val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    // Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 16.dp, bottom = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Transfer Vault & Logs",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
        Text(
          text = "Cryptographic audit trail of completed WLAN transfers",
          style = MaterialTheme.typography.bodySmall,
          color = TextMuted
        )
      }

      if (historyList.isNotEmpty()) {
        IconButton(
          onClick = { showClearConfirm = true },
          modifier = Modifier.testTag("clear_history_button")
        ) {
          Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear History", tint = AlertCrimson)
        }
      }
    }

    // Filter Chips
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      listOf("ALL" to "All Transfers", "RECEIVE" to "Received", "SEND" to "Sent").forEach { (key, label) ->
        val selected = filterDirection == key
        FilterChip(
          selected = selected,
          onClick = { filterDirection = key },
          label = { Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF162550),
            selectedLabelColor = CyberCyan,
            containerColor = DarkSurface,
            labelColor = TextMuted
          ),
          border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = DarkBorder,
            selectedBorderColor = CyberCyan
          )
        )
      }
    }

    if (filteredList.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(24.dp)
        ) {
          Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = TextMuted.copy(alpha = 0.5f),
            modifier = Modifier.size(56.dp)
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "No Transfers Recorded Yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          Text(
            text = "Files sent or received over WLAN with end-to-end encryption will be logged here.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(filteredList, key = { it.id }) { record ->
          TransferRecordCard(
            record = record,
            dateFormat = dateFormat,
            onOpen = {
              if (record.localFilePath != null) {
                openFile(context, record.localFilePath)
              }
            },
            onDelete = { viewModel.deleteHistory(record) }
          )
        }

        item {
          Spacer(modifier = Modifier.height(80.dp))
        }
      }
    }
  }

  if (showClearConfirm) {
    AlertDialog(
      onDismissRequest = { showClearConfirm = false },
      containerColor = DarkSurface,
      title = { Text("Clear All Transfer Logs?", color = Color.White, fontWeight = FontWeight.Bold) },
      text = { Text("This will permanently clear your cryptographic transfer audit history.", color = TextMuted) },
      confirmButton = {
        Button(
          onClick = {
            viewModel.clearAllHistory()
            showClearConfirm = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = AlertCrimson),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("Clear All")
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { showClearConfirm = false },
          colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
          border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
fun TransferRecordCard(
  record: TransferRecord,
  dateFormat: SimpleDateFormat,
  onOpen: () -> Unit,
  onDelete: () -> Unit
) {
  val isReceive = record.direction == "RECEIVE"
  val dateStr = remember(record.timestamp) { dateFormat.format(Date(record.timestamp)) }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("transfer_record_${record.id}")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      // Top row: Direction icon, file name, delete button
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(if (isReceive) Color(0xFF004D25) else Color(0xFF003848)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (isReceive) Icons.AutoMirrored.Filled.CallReceived else Icons.AutoMirrored.Filled.CallMade,
              contentDescription = null,
              tint = if (isReceive) ShieldEmerald else CyberCyan,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = record.fileName,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "${if (isReceive) "From" else "To"}: ${record.peerName} (${record.peerIp})",
              style = MaterialTheme.typography.labelSmall,
              color = TextMuted
            )
          }
        }

        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
          Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(18.dp))
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Stats row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(DarkSurface, RoundedCornerShape(10.dp))
          .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = FileUtils.formatFileSize(record.fileSize),
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = Color.White
        )
        Text(
          text = String.format("%.1f MB/s", record.avgSpeedMbps),
          style = MaterialTheme.typography.labelSmall,
          color = CyberCyan,
          fontWeight = FontWeight.SemiBold
        )
        Text(
          text = "${record.transferDurationMs / 1000.0}s",
          style = MaterialTheme.typography.labelSmall,
          color = TextMuted
        )
        Text(
          text = "SAS: ${record.sasCode}",
          style = MaterialTheme.typography.labelSmall,
          color = ShieldEmerald,
          fontFamily = FontFamily.Monospace
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      // SHA-256 and Timestamp
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = dateStr,
          style = MaterialTheme.typography.labelSmall,
          color = TextMuted,
          fontSize = 10.sp
        )

        if (record.localFilePath != null) {
          Button(
            onClick = onOpen,
            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color(0xFF00363D)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(30.dp)
          ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Open", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

private fun openFile(context: Context, filePath: String) {
  try {
    val file = File(filePath)
    if (!file.exists()) {
      Toast.makeText(context, "File does not exist on disk", Toast.LENGTH_SHORT).show()
      return
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val mimeType = context.contentResolver.getType(uri) ?: "*/*"
    val intent = Intent(Intent.ACTION_VIEW).apply {
      setDataAndType(uri, mimeType)
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(intent, "Open file with"))
  } catch (e: Exception) {
    Toast.makeText(context, "Could not open file: ${e.message}", Toast.LENGTH_SHORT).show()
  }
}
