package com.mkz.ytfm.ui.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mkz.ytfm.controller.FruitDropController
import com.mkz.ytfm.model.FruitType
import kotlin.math.roundToInt

@Composable
fun GuideMarkersOverlay() {
    val config by FruitDropController.config.collectAsState()
    val detection by FruitDropController.currentDetection.collectAsState()

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
        // Draw hollow ring JUST OUTSIDE the sampled pixels so ring color never contaminates scan
        val ringRadiusPx = scanRadiusPx + 6f

        val ringColor = if (detection.fruit != FruitType.UNKNOWN) {
            detection.fruit.composeColor
        } else {
            Color(0xFF00E5FF)
        }

        // Draw guide dashed lines & hollow scan target ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)

            if (config.showMarkers) {
                drawLine(
                    color = Color(0x9976BA1B),
                    start = Offset(leftX, dropY),
                    end = Offset(leftX, heightPx * 0.90f),
                    strokeWidth = 3f,
                    pathEffect = dashEffect
                )

                drawLine(
                    color = Color(0x99FB8C00),
                    start = Offset(rightX, dropY),
                    end = Offset(rightX, heightPx * 0.90f),
                    strokeWidth = 3f,
                    pathEffect = dashEffect
                )
            }

            if (config.showScanBox) {
                // Outer hollow ring (100% transparent inside so pixels are pure)
                drawCircle(
                    color = ringColor,
                    radius = ringRadiusPx,
                    center = Offset(scanX, scanY),
                    style = Stroke(width = 4f)
                )
            }
        }

        if (config.showMarkers) {
            val circleSizeDp = 36.dp
            var leftLabelSize by remember { mutableStateOf(IntSize.Zero) }
            var rightLabelSize by remember { mutableStateOf(IntSize.Zero) }

            // KIRI target circle centered exactly on (leftX, dropY)
            Box(
                modifier = Modifier
                    .offset {
                        val halfPx = (circleSizeDp.toPx() / 2f).roundToInt()
                        IntOffset((leftX - halfPx).roundToInt(), (dropY - halfPx).roundToInt())
                    }
                    .size(circleSizeDp)
                    .clip(CircleShape)
                    .background(Color(0xAA76BA1B))
                    .border(2.dp, Color.White, CircleShape)
                    .pointerInput(widthPx, heightPx) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newX = (leftX + dragAmount.x).coerceIn(20f, widthPx * 0.45f)
                            val newY = (dropY + dragAmount.y).coerceIn(heightPx * 0.2f, heightPx * 0.75f)
                            FruitDropController.updateCoordinates(
                                dropLeftRelX = newX / widthPx,
                                dropRelY = newY / heightPx
                            )
                        }
                    }
            )

            Text(
                text = "KIRI",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .onSizeChanged { leftLabelSize = it }
                    .offset {
                        IntOffset(
                            (leftX - leftLabelSize.width / 2f).roundToInt(),
                            (dropY - circleSizeDp.toPx() / 2f - leftLabelSize.height - 4.dp.toPx()).roundToInt()
                        )
                    }
            )

            // KANAN target circle centered exactly on (rightX, dropY)
            Box(
                modifier = Modifier
                    .offset {
                        val halfPx = (circleSizeDp.toPx() / 2f).roundToInt()
                        IntOffset((rightX - halfPx).roundToInt(), (dropY - halfPx).roundToInt())
                    }
                    .size(circleSizeDp)
                    .clip(CircleShape)
                    .background(Color(0xAAFB8C00))
                    .border(2.dp, Color.White, CircleShape)
                    .pointerInput(widthPx, heightPx) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newX = (rightX + dragAmount.x).coerceIn(widthPx * 0.55f, widthPx - 20f)
                            val newY = (dropY + dragAmount.y).coerceIn(heightPx * 0.2f, heightPx * 0.75f)
                            FruitDropController.updateCoordinates(
                                dropRightRelX = newX / widthPx,
                                dropRelY = newY / heightPx
                            )
                        }
                    }
            )

            Text(
                text = "KANAN",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .onSizeChanged { rightLabelSize = it }
                    .offset {
                        IntOffset(
                            (rightX - rightLabelSize.width / 2f).roundToInt(),
                            (dropY - circleSizeDp.toPx() / 2f - rightLabelSize.height - 4.dp.toPx()).roundToInt()
                        )
                    }
            )
        }

        if (config.showScanBox) {
            var labelSize by remember { mutableStateOf(IntSize.Zero) }
            val hitSizeDp = (config.scanRadiusDp * 2 + 12).dp

            // Invisible centered drag hit-box over the hollow scan ring (no colored fill!)
            Box(
                modifier = Modifier
                    .offset {
                        val halfPx = (hitSizeDp.toPx() / 2f).roundToInt()
                        IntOffset((scanX - halfPx).roundToInt(), (scanY - halfPx).roundToInt())
                    }
                    .size(hitSizeDp)
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
                    }
            )

            // Label placed BELOW the scan ring so inside of circle & timer above are 100% unobstructed
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .onSizeChanged { labelSize = it }
                    .offset {
                        IntOffset(
                            (scanX - labelSize.width / 2f).roundToInt().coerceAtLeast(4),
                            (scanY + ringRadiusPx + 4.dp.toPx()).roundToInt()
                        )
                    }
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xB30F172A))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (detection.fruit != FruitType.UNKNOWN) detection.fruit.displayName else "SCAN BUAH",
                    color = ringColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
