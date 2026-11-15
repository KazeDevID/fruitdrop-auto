package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.Display
import com.example.controller.FruitDropController

class FruitDropAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "FruitDropAccess"
        var instance: FruitDropAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        FruitDropController.setAccessibilityConnected(true)
        Log.d(TAG, "FruitDropAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {
        // No action needed for raw accessibility events
    }

    override fun onInterrupt() {
        Log.w(TAG, "FruitDropAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        FruitDropController.setAccessibilityConnected(false)
        Log.d(TAG, "FruitDropAccessibilityService destroyed")
    }

    /**
     * Dispatches an ultra-fast tap gesture at (x, y).
     * Duration of 35ms gives native tap feel with no lag.
     */
    fun performClick(x: Float, y: Float, onComplete: ((Boolean) -> Unit)? = null) {
        val clickPath = Path().apply {
            moveTo(x, y)
        }

        val stroke = GestureDescription.StrokeDescription(clickPath, 0, 35)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val dispatched = dispatchGesture(
            gesture,
            object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    onComplete?.invoke(true)
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    onComplete?.invoke(false)
                }
            },
            null
        )

        if (!dispatched) {
            onComplete?.invoke(false)
        }
    }

    /**
     * Fallback screenshot on Android 11+ (API 30+) using AccessibilityService.
     */
    fun captureScreen(onCaptured: (Bitmap?) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshot: ScreenshotResult) {
                        try {
                            val hardwareBuffer = screenshot.hardwareBuffer
                            val colorSpace = screenshot.colorSpace
                            val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)
                            hardwareBuffer.close()
                            onCaptured(bitmap)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing screenshot", e)
                            onCaptured(null)
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        Log.e(TAG, "Accessibility takeScreenshot failed: $errorCode")
                        onCaptured(null)
                    }
                }
            )
        } else {
            onCaptured(null)
        }
    }
}
