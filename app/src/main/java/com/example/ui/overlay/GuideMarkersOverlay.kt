package com.example.ui.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.controller.FruitDropController
import kotlin.math.roundToInt

@Composable
fun GuideMarkersOverlay() {
    val config by FruitDropController.config.collectAsState()

    if (!config.showMarkers && !config.showScanBox) return

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        if (widthPx <= 0 || heightPx <= 0) return@BoxWithConstraints

        val leftX = config.dropLeftRelX * widthPx
        val rightX = config.dropRightRelX * widthPx
        val dropY = config.dropRelY * heightPx

        val scanX = config.scanRelX * widthPx
        val scanY = config.scanRelY * heightPx
        val scanRadiusPx = config.scanRadiusDp * (widthPx / 360f)

        // Draw dashed lines for columns and circles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)

            if (config.showMarkers) {
                // Left Column Dashed Line
                drawLine(
                    color = Color(0x9976BA1B),
                    start = Offset(leftX, dropY),
                    end = Offset(leftX, heightPx * 0.90f),
                    strokeWidth = 3f,
                    pathEffect = dashEffect
                )

                // Right Column Dashed Line
                drawLine(
                    color = Color(0x99FB8C00),
                    start = Offset(rightX, dropY),
                    end = Offset(rightX, heightPx * 0.90f),
                    strokeWidth = 3f,
                    pathEffect = dashEffect
                )
            }

            if (config.showScanBox) {
                // Scan target ring
                drawCircle(
                    color = Color(0xCC00E5FF),
                    radius = scanRadiusPx,
                    center = Offset(scanX, scanY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 4f,
                        pathEffect = dashEffect
                    )
                )
            }
        }

        if (config.showMarkers) {
            // "KIRI" label & Draggable Circle
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (leftX - 32.dp.toPx()).roundToInt(),
                            (dropY - 48.dp.toPx()).roundToInt()
                        )
                    }
                    .pointerInput(widthPx, heightPx) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newX = (leftX + dragAmount.x).coerceIn(20f, widthPx * 0.45f)
                            val newY = (dropY + dragAmount.y).coerceIn(heightPx * 0.2f, heightPx * 0.7f)
                            FruitDropController.updateCoordinates(
                                dropLeftRelX = newX / widthPx,
                                dropRelY = newY / heightPx
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.layout.Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "KIRI",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xCC76BA1B))
                    )
                }
            }

            // "KANAN" label & Draggable Circle
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (rightX - 32.dp.toPx()).roundToInt(),
                            (dropY - 48.dp.toPx()).roundToInt()
                        )
                    }
                    .pointerInput(widthPx, heightPx) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newX = (rightX + dragAmount.x).coerceIn(widthPx * 0.55f, widthPx - 20f)
                            val newY = (dropY + dragAmount.y).coerceIn(heightPx * 0.2f, heightPx * 0.7f)
                            FruitDropController.updateCoordinates(
                                dropRightRelX = newX / widthPx,
                                dropRelY = newY / heightPx
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.layout.Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "KANAN",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xCCFB8C00))
                    )
                }
            }
        }

        if (config.showScanBox) {
            // "SCAN BUAH" Label & Draggable Scanner Target
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (scanX - 40.dp.toPx()).roundToInt(),
                            (scanY - 42.dp.toPx()).roundToInt()
                        )
                    }
                    .pointerInput(widthPx, heightPx) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newX = (scanX + dragAmount.x).coerceIn(10f, widthPx - 10f)
                            val newY = (scanY + dragAmount.y).coerceIn(heightPx * 0.1f, heightPx * 0.8f)
                            FruitDropController.updateCoordinates(
                                scanRelX = newX / widthPx,
                                scanRelY = newY / heightPx
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.layout.Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SCAN BUAH",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0x5500E5FF))
                    )
                }
            }
        }
    }
}
