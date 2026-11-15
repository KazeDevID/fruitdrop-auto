package com.example.detector

import android.graphics.Bitmap
import android.graphics.Color
import com.example.model.DetectionResult
import com.example.model.DropTarget
import com.example.model.FruitDropConfig
import com.example.model.FruitType

object FruitDetector {

    /**
     * Classifies a single HSV color into the corresponding FruitType.
     */
    fun classifyHsv(hue: Float, sat: Float, value: Float): FruitType {
        return when {
            // Blueberry: violet / deep indigo / purple
            hue in 215f..275f && sat >= 0.28f -> FruitType.BLUEBERRY

            // Kiwi: vibrant lime green (Yellow-Green)
            hue in 65f..135f && sat >= 0.22f -> FruitType.KIWI_HIJAU

            // Peach: soft warm coral/pastel peach (with high brightness and moderate saturation)
            ((hue in 10f..35f) || (hue in 345f..360f)) && value >= 0.70f && sat in 0.15f..0.65f -> FruitType.PEACH

            // Strawberry: vivid red with high saturation
            ((hue in 340f..360f) || (hue in 0f..15f)) && sat >= 0.55f -> FruitType.STRAWBERRY

            // Orange: bright orange/amber
            hue in 25f..60f && sat >= 0.50f -> FruitType.ORANGE

            else -> FruitType.UNKNOWN
        }
    }

    /**
     * Ultra-fast real-time detection of the fruit currently inside the scan area.
     * Samples pixels in a circular radius, ignores background sky-blue and white border,
     * and maps the dominant fruit hue to the exact fruit type.
     */
    fun detectFruit(
        bitmap: Bitmap,
        config: FruitDropConfig
    ): DetectionResult {
        val width = bitmap.width
        val height = bitmap.height

        val centerX = (config.scanRelX * width).toInt().coerceIn(0, width - 1)
        val centerY = (config.scanRelY * height).toInt().coerceIn(0, height - 1)
        val sampleRadius = (config.scanRadiusDp * (width / 360f)).toInt().coerceIn(10, 80)

        var totalHue = 0f
        var totalSat = 0f
        var totalVal = 0f
        var validPixelCount = 0

        val hsv = FloatArray(3)

        val xMin = (centerX - sampleRadius).coerceAtLeast(0)
        val xMax = (centerX + sampleRadius).coerceAtMost(width - 1)
        val yMin = (centerY - sampleRadius).coerceAtLeast(0)
        val yMax = (centerY + sampleRadius).coerceAtMost(height - 1)

        val radiusSquared = sampleRadius * sampleRadius
        val step = 2

        val voteCounts = mutableMapOf<FruitType, Int>()

        for (y in yMin..yMax step step) {
            val dy = y - centerY
            for (x in xMin..xMax step step) {
                val dx = x - centerX
                if (dx * dx + dy * dy > radiusSquared) continue

                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                Color.RGBToHSV(r, g, b, hsv)
                val hue = hsv[0]
                val sat = hsv[1]
                val value = hsv[2]

                // Filter out transparent or fully black/uninitialized pixels
                if (value < 0.15f) continue

                // Filter out white/light-gray circle border
                if (sat < 0.18f && value > 0.70f) continue

                // Filter out Yeetalk light sky-blue background: hue ~190° - 215°
                if (hue in 190f..215f && sat in 0.15f..0.55f && value > 0.75f) continue

                val classified = classifyHsv(hue, sat, value)
                if (classified != FruitType.UNKNOWN) {
                    voteCounts[classified] = (voteCounts[classified] ?: 0) + 1
                }

                totalHue += hue
                totalSat += sat
                totalVal += value
                validPixelCount++
            }
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

        // Best candidate with highest votes
        val maxEntry = voteCounts.maxByOrNull { it.value }
        val detectedFruit = if (maxEntry != null && maxEntry.value >= 3) {
            maxEntry.key
        } else {
            classifyHsv(avgHue, avgSat, avgVal)
        }

        val target = config.getTargetForFruit(detectedFruit)
        val confidence = if (detectedFruit != FruitType.UNKNOWN) 0.95f else 0.0f

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
