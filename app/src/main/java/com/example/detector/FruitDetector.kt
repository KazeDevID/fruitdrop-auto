package com.example.detector

import android.graphics.Bitmap
import android.graphics.Color
import com.example.model.DetectionResult
import com.example.model.DropTarget
import com.example.model.FruitDropConfig
import com.example.model.FruitType

object FruitDetector {

    /**
     * Classifies a single HSV color into the corresponding Yeetalk FruitType.
     * Calibrated directly against real Yeetalk Fruit Merge pixel samples.
     */
    fun classifyHsv(hue: Float, sat: Float, value: Float): FruitType {
        // Ignore dark outlines, black seeds/eyes, and uninitialized pixels
        if (value < 0.20f) return FruitType.UNKNOWN

        // Ignore white/light-gray circle background & highlights
        if (sat < 0.16f && value > 0.65f) return FruitType.UNKNOWN

        // Ignore Yeetalk light sky-blue background & cyan scan ring
        if (hue in 182f..216f && sat <= 0.68f && value >= 0.62f) return FruitType.UNKNOWN

        return when {
            // Blueberry: deep indigo / blue-purple (H: 216..275)
            hue in 216f..275f && sat >= 0.30f -> FruitType.BLUEBERRY

            // Grape: purple / magenta (H: 275..331)
            hue in 275.01f..331f && sat >= 0.32f && value >= 0.30f -> FruitType.GRAPE

            // Kiwi: bright lime/yellow-green flesh (H: 65..145)
            hue in 65f..145f && sat >= 0.22f && value >= 0.25f -> FruitType.KIWI_HIJAU

            // Peach: warm pastel salmon/apricot (H: 2..26, moderate saturation, high brightness)
            hue in 2f..26f && sat in 0.18f..0.49f && value >= 0.78f -> FruitType.PEACH

            // Strawberry / Cherry: crimson/pinkish red (H >= 332 or vivid red H <= 16)
            (hue >= 331.01f || (hue <= 16f && sat >= 0.50f)) && sat >= 0.45f && value >= 0.40f ->
                FruitType.STRAWBERRY

            // Orange / Tangerine: vivid orange / amber / yellow-orange
            hue in 22f..64.99f && sat >= 0.50f && value >= 0.45f -> FruitType.ORANGE

            else -> FruitType.UNKNOWN
        }
    }

    /**
     * Ultra-fast real-time detection of the fruit inside the scan area.
     * Uses bulk getPixels() into a compact square buffer for sub-millisecond scanning.
     */
    fun detectFruit(
        bitmap: Bitmap,
        config: FruitDropConfig
    ): DetectionResult {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) {
            return DetectionResult(FruitType.UNKNOWN, DropTarget.NONE, 0f, 0f, 0f, 0f)
        }

        val centerX = (config.scanRelX * width).toInt().coerceIn(0, width - 1)
        val centerY = (config.scanRelY * height).toInt().coerceIn(0, height - 1)
        val sampleRadius = (config.scanRadiusDp * (width / 360f)).toInt().coerceIn(8, 70)

        val xMin = (centerX - sampleRadius).coerceAtLeast(0)
        val xMax = (centerX + sampleRadius).coerceAtMost(width - 1)
        val yMin = (centerY - sampleRadius).coerceAtLeast(0)
        val yMax = (centerY + sampleRadius).coerceAtMost(height - 1)

        val boxW = (xMax - xMin + 1).coerceAtLeast(1)
        val boxH = (yMax - yMin + 1).coerceAtLeast(1)
        val pixels = IntArray(boxW * boxH)
        bitmap.getPixels(pixels, 0, boxW, xMin, yMin, boxW, boxH)

        var totalHue = 0f
        var totalSat = 0f
        var totalVal = 0f
        var validPixelCount = 0

        val hsv = FloatArray(3)
        val radiusSquared = sampleRadius * sampleRadius
        val step = 2

        val voteCounts = mutableMapOf<FruitType, Int>()

        var yOffset = 0
        while (yOffset < boxH) {
            val dy = (yMin + yOffset) - centerY
            val dySq = dy * dy
            var xOffset = 0
            while (xOffset < boxW) {
                val dx = (xMin + xOffset) - centerX
                if (dx * dx + dySq <= radiusSquared) {
                    val pixel = pixels[yOffset * boxW + xOffset]
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)

                    Color.RGBToHSV(r, g, b, hsv)
                    val hue = hsv[0]
                    val sat = hsv[1]
                    val value = hsv[2]

                    val classified = classifyHsv(hue, sat, value)
                    if (classified != FruitType.UNKNOWN) {
                        voteCounts[classified] = (voteCounts[classified] ?: 0) + 1
                        totalHue += hue
                        totalSat += sat
                        totalVal += value
                        validPixelCount++
                    }
                }
                xOffset += step
            }
            yOffset += step
        }

        if (validPixelCount < 5) {
            return DetectionResult(
                fruit = FruitType.UNKNOWN,
                target = DropTarget.NONE,
                confidence = 0f,
                dominantHue = 0f,
                dominantSat = 0f,
                dominantVal = 0f
            )
        }

        val avgHue = totalHue / validPixelCount
        val avgSat = totalSat / validPixelCount
        val avgVal = totalVal / validPixelCount

        val maxEntry = voteCounts.maxByOrNull { it.value }
        val detectedFruit = if (maxEntry != null && maxEntry.value >= 5) {
            maxEntry.key
        } else {
            FruitType.UNKNOWN
        }

        val target = config.getTargetForFruit(detectedFruit)
        val confidence = if (detectedFruit != FruitType.UNKNOWN && maxEntry != null) {
            (maxEntry.value.toFloat() / validPixelCount.toFloat()).coerceIn(0f, 1f)
        } else {
            0.0f
        }

        return DetectionResult(
            fruit = detectedFruit,
            target = target,
            confidence = confidence,
            dominantHue = avgHue,
            dominantSat = avgSat,
            dominantVal = avgVal
        )
    }
}
