package com.example.ui.overlay

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.controller.FruitDropController
import com.example.model.DropTarget
import com.example.model.FruitDropConfig
import com.example.model.FruitType
import com.example.model.QueueStage
import com.example.model.ScanMode

@Composable
fun FloatingPanelOverlay(
    onDrag: (dx: Float, dy: Float) -> Unit,
    onClose: () -> Unit
) {
    val config by FruitDropController.config.collectAsState()
    val detection by FruitDropController.currentDetection.collectAsState()

    Surface(
        modifier = Modifier
            .wrapContentSize()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF384B66), RoundedCornerShape(16.dp))
            .testTag("floating_panel"),
        color = Color(0xF0101827),
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .width(214.dp)
                .padding(12.dp)
        ) {
            // Header / Drag handle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.x, dragAmount.y)
                        }
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (config.autoPlayEnabled) Color(0xFF00E676) else Color(0xFFFF5252))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FruitDrop",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { FruitDropController.setMinimized(true) },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("btn_minimize")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Minimize",
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("btn_close_overlay")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Auto Play Button (matching user's screenshot)
            val buttonBg by animateColorAsState(
                targetValue = if (config.autoPlayEnabled) Color(0xFF00C853) else Color(0xFF263238),
                label = "btn_bg"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(buttonBg)
                    .clickable { FruitDropController.toggleAutoPlay() }
                    .testTag("btn_toggle_autoplay"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (config.autoPlayEnabled) "● ON • AUTO AKTIF" else "○ OFF • AUTO MATI",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Current Fruit Detection Status & Queue Indicator
            val detectedFruit = detection.fruit
            val pct = (detection.confidence * 100).toInt()
            val isWaitingFirstManual =
                config.autoPlayEnabled &&
                    config.scanMode == ScanMode.NEXT_QUEUE &&
                    config.queueStage == QueueStage.WAITING_FIRST_MANUAL

            val badgeFruit = when {
                config.autoPlayEnabled && config.scanMode == ScanMode.NEXT_QUEUE && config.queuedFruit != FruitType.UNKNOWN ->
                    config.queuedFruit
                else -> detectedFruit
            }
            val targetText = when (config.getTargetForFruit(badgeFruit)) {
                DropTarget.LEFT -> "Kiri"
                DropTarget.RIGHT -> "Kanan"
                DropTarget.NONE -> "Siaga"
            }

            val statusText = when {
                isWaitingFirstManual && config.queuedFruit != FruitType.UNKNOWN ->
                    "Drop Buah 1 Manual • Next: ${config.queuedFruit.displayName}"
                isWaitingFirstManual ->
                    "Drop Buah 1 Manual (Siapkan...)"
                config.autoPlayEnabled && config.scanMode == ScanMode.NEXT_QUEUE && config.queuedFruit != FruitType.UNKNOWN ->
                    "Drop: ${config.queuedFruit.displayName} → $targetText"
                detectedFruit != FruitType.UNKNOWN ->
                    "${detectedFruit.displayName} → $targetText ($pct%)"
                detection.dominantHue > 0f ->
                    "Memindai... (H:${detection.dominantHue.toInt()}°)"
                else ->
                    "Memindai Buah..."
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isWaitingFirstManual) Color(0xFF3E2723) else Color(0xFF1E293B))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(badgeFruit.composeColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2
                    )
                }

                if (config.autoPlayEnabled && config.scanMode == ScanMode.NEXT_QUEUE && config.queueStage == QueueStage.AUTO_RUNNING && detectedFruit != FruitType.UNKNOWN) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Terbaca Next: ${detectedFruit.displayName} ($pct%)",
                        color = Color(0xFF90CAF9),
                        fontSize = 10.sp
                    )
                }

                if (isWaitingFirstManual) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFF9800))
                            .clickable { FruitDropController.confirmFirstManualDrop() }
                            .padding(vertical = 5.dp)
                            .testTag("btn_confirm_manual_drop"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sudah Drop Buah 1 ▶",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stats row: Kiri: X • Kanan: Y + Reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Kiri: ${config.leftCount} • Kanan: ${config.rightCount}",
                    color = Color(0xFFB0BEC5),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                IconButton(
                    onClick = { FruitDropController.resetCounters() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Stats",
                        tint = Color(0xFF90A4AE),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Toggle Penanda Titik (Matching Image 1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (config.showMarkers) Color(0xFF1B3A57) else Color(0xFF192231))
                    .clickable { FruitDropController.toggleMarkers() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("toggle_penanda_titik"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Penanda Titik",
                    color = Color.White,
                    fontSize = 11.sp
                )
                Text(
                    text = if (config.showMarkers) "ON" else "OFF",
                    color = if (config.showMarkers) Color(0xFF40C4FF) else Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Toggle Scan Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (config.showScanBox) Color(0xFF1B3A57) else Color(0xFF192231))
                    .clickable { FruitDropController.toggleScanBox() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("toggle_scan_box"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Target Scanner",
                    color = Color.White,
                    fontSize = 11.sp
                )
                Text(
                    text = if (config.showScanBox) "ON" else "OFF",
                    color = if (config.showScanBox) Color(0xFF40C4FF) else Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Toggle Scan Mode (Antrean Next Fruit vs Langsung Awan)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (config.scanMode == ScanMode.NEXT_QUEUE) Color(0xFF163C2B) else Color(0xFF1B3A57))
                    .clickable { FruitDropController.toggleScanMode() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("toggle_scan_mode"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mode Scan",
                    color = Color.White,
                    fontSize = 11.sp
                )
                Text(
                    text = if (config.scanMode == ScanMode.NEXT_QUEUE) "ANTREAN (NEXT)" else "LANGSUNG",
                    color = if (config.scanMode == ScanMode.NEXT_QUEUE) Color(0xFF00E676) else Color(0xFF40C4FF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Toggle Calibration Drag Mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (config.isCalibrating) Color(0xFF4A3200) else Color(0xFF192231))
                    .clickable { FruitDropController.toggleCalibrating() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("toggle_calibration"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Geser Titik",
                    color = Color.White,
                    fontSize = 11.sp
                )
                Text(
                    text = if (config.isCalibrating) "TERBUKA" else "TERKUNCI",
                    color = if (config.isCalibrating) Color(0xFFFFD54F) else Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MinimizedBubbleOverlay(
    onDrag: (dx: Float, dy: Float) -> Unit,
    onClick: () -> Unit
) {
    val config by FruitDropController.config.collectAsState()

    Surface(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .border(2.dp, if (config.autoPlayEnabled) Color(0xFF00E676) else Color(0xFFFF9800), CircleShape)
            .clickable { onClick() }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
            .testTag("minimized_bubble"),
        color = Color(0xEB111827),
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (config.autoPlayEnabled) Color(0xFF00E676) else Color(0xFFFF9800))
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (config.autoPlayEnabled) "ON" else "OFF",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
