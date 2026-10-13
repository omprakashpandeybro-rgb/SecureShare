package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.PeerDevice
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceContainer
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ShieldEmerald
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarScanner(
  peers: List<PeerDevice>,
  localDeviceName: String,
  onPeerSelected: (PeerDevice) -> Unit,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "RadarTransition")
  val rotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 4000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "RadarSweep"
  )
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.3f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2500, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "RadarPulse"
  )

  BoxWithConstraints(
    modifier = modifier
      .fillMaxWidth()
      .height(280.dp)
      .clip(RoundedCornerShape(24.dp))
      .background(DarkSurfaceElevated)
      .border(1.dp, DarkBorder, RoundedCornerShape(24.dp)),
    contentAlignment = Alignment.Center
  ) {
    val radarRadius = maxWidth.coerceAtMost(maxHeight) / 2 - 20.dp

    // Canvas Radar Rings & Sweeping Beam
    Canvas(modifier = Modifier.fillMaxSize()) {
      val center = Offset(size.width / 2, size.height / 2)
      val maxR = size.minDimension / 2 - 24.dp.toPx()

      // Concentric grid circles
      val rings = 3
      for (i in 1..rings) {
        val r = (maxR / rings) * i
        drawCircle(
          color = Color(0xFF1E2D5A),
          radius = r,
          center = center,
          style = Stroke(width = 1.5.dp.toPx())
        )
      }

      // Pulsing wave ring
      drawCircle(
        color = CyberCyan.copy(alpha = (1f - pulseScale) * 0.4f),
        radius = maxR * pulseScale,
        center = center,
        style = Stroke(width = 2.dp.toPx())
      )

      // Crosshairs
      drawLine(
        color = Color(0xFF1E2D5A),
        start = Offset(center.x - maxR, center.y),
        end = Offset(center.x + maxR, center.y),
        strokeWidth = 1.dp.toPx()
      )
      drawLine(
        color = Color(0xFF1E2D5A),
        start = Offset(center.x, center.y - maxR),
        end = Offset(center.x, center.y + maxR),
        strokeWidth = 1.dp.toPx()
      )

      // Sweeping Beam line
      val rad = Math.toRadians(rotation.toDouble())
      val beamEnd = Offset(
        x = center.x + (maxR * cos(rad)).toFloat(),
        y = center.y + (maxR * sin(rad)).toFloat()
      )
      drawLine(
        brush = Brush.radialGradient(
          colors = listOf(CyberCyan, Color.Transparent),
          center = center,
          radius = maxR
        ),
        start = center,
        end = beamEnd,
        strokeWidth = 2.5.dp.toPx()
      )
    }

    // Center Node (You)
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.align(Alignment.Center)
    ) {
      Box(
        modifier = Modifier
          .size(54.dp)
          .clip(CircleShape)
          .background(Brush.linearGradient(listOf(Color(0xFF005234), Color(0xFF00363D))))
          .border(2.dp, ShieldEmerald, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Shield,
          contentDescription = "Local Device",
          tint = ShieldEmerald,
          modifier = Modifier.size(26.dp)
        )
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "This Device",
        style = MaterialTheme.typography.labelSmall,
        color = CyberCyan,
        fontWeight = FontWeight.Bold
      )
    }

    // Render Discovered Peers positioned in radar orbits
    if (peers.isEmpty()) {
      Box(
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 12.dp)
          .background(DarkSurface.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
          .padding(horizontal = 12.dp, vertical = 6.dp)
      ) {
        Text(
          text = "Scanning LAN for nearby CipherDrop devices...",
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFF94A3B8)
        )
      }
    } else {
      // Place peers around the radar using deterministic polar coordinates
      peers.forEachIndexed { index, peer ->
        val angleRad = Math.toRadians((index * (360.0 / peers.size) + 45.0))
        val distFraction = 0.65f + (index % 2) * 0.15f
        val xOffset = ((radarRadius.value * distFraction) * cos(angleRad)).dp
        val yOffset = ((radarRadius.value * distFraction) * sin(angleRad)).dp

        Box(
          modifier = Modifier
            .offset(x = xOffset, y = yOffset)
            .clickable { onPeerSelected(peer) }
            .testTag("peer_node_${peer.id}"),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
              modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0xFF15224A))
                .border(2.dp, CyberCyan, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              val icon = when (peer.deviceType) {
                "TABLET" -> Icons.Default.Tablet
                "DESKTOP" -> Icons.Default.Laptop
                else -> Icons.Default.PhoneAndroid
              }
              Icon(
                imageVector = icon,
                contentDescription = peer.name,
                tint = CyberCyan,
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = DarkSurface.copy(alpha = 0.9f)
            ) {
              Text(
                text = peer.name,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }
      }
    }
  }
}
