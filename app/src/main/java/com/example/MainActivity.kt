package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ActiveTransferOverlay
import com.example.ui.components.IncomingTransferDialog
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SecurityScreen
import com.example.ui.screens.ShareScreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ShieldEmerald
import com.example.ui.theme.TextMuted
import com.example.viewmodel.ShareViewModel

enum class AppTab(val label: String, val activeIcon: ImageVector, val inactiveIcon: ImageVector) {
  SHARE("Radar Share", Icons.Filled.Radar, Icons.Outlined.Radar),
  HISTORY("Transfer Logs", Icons.Filled.History, Icons.Outlined.History),
  SECURITY("E2EE Security", Icons.Filled.Security, Icons.Outlined.Security)
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme(darkTheme = true) {
        CipherDropApp()
      }
    }
  }
}

@Composable
fun CipherDropApp(
  viewModel: ShareViewModel = viewModel()
) {
  var selectedTab by rememberSaveable { mutableStateOf(AppTab.SHARE) }

  val transferStatus by viewModel.transferStatus.collectAsStateWithLifecycle()
  val incomingRequest by viewModel.incomingRequest.collectAsStateWithLifecycle()

  // Handle system back navigation to return to the primary tab
  BackHandler(enabled = selectedTab != AppTab.SHARE) {
    selectedTab = AppTab.SHARE
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    contentWindowInsets = WindowInsets.safeDrawing,
    containerColor = MaterialTheme.colorScheme.background,
    bottomBar = {
      NavigationBar(
        modifier = Modifier
          .windowInsetsPadding(WindowInsets.navigationBars)
          .testTag("main_navigation_bar"),
        containerColor = DarkSurface,
        tonalElevation = androidx.compose.ui.unit.Dp(8f)
      ) {
        AppTab.values().forEach { tab ->
          val selected = selectedTab == tab
          NavigationBarItem(
            selected = selected,
            onClick = { selectedTab = tab },
            icon = {
              Icon(
                imageVector = if (selected) tab.activeIcon else tab.inactiveIcon,
                contentDescription = tab.label
              )
            },
            label = {
              Text(
                text = tab.label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
              )
            },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = Color(0xFF003822),
              selectedTextColor = CyberCyan,
              indicatorColor = ShieldEmerald,
              unselectedIconColor = TextMuted,
              unselectedTextColor = TextMuted
            ),
            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
          )
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Primary Tab Screens
      when (selectedTab) {
        AppTab.SHARE -> ShareScreen(viewModel = viewModel)
        AppTab.HISTORY -> HistoryScreen(viewModel = viewModel)
        AppTab.SECURITY -> SecurityScreen(viewModel = viewModel)
      }

      // Global Live Transfer HUD / Overlay
      ActiveTransferOverlay(
        transferStatus = transferStatus,
        onDismiss = { viewModel.dismissTransferStatus() },
        modifier = Modifier.align(Alignment.BottomCenter)
      )

      // Global Incoming Transfer Prompt Dialog
      incomingRequest?.let { request ->
        IncomingTransferDialog(
          request = request,
          onAccept = { viewModel.acceptIncomingRequest() },
          onDecline = { viewModel.declineIncomingRequest() }
        )
      }
    }
  }
}
