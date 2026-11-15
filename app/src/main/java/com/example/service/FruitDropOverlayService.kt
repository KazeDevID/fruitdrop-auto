package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.R
import com.example.controller.FruitDropController
import com.example.detector.FruitDetector
import com.example.model.DropTarget
import com.example.model.FruitType
import com.example.ui.overlay.FloatingPanelOverlay
import com.example.ui.overlay.GuideMarkersOverlay
import com.example.ui.overlay.MinimizedBubbleOverlay
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

class FruitDropOverlayService : Service() {

    companion object {
        private const val TAG = "FruitDropOverlay"
        private const val NOTIFICATION_CHANNEL_ID = "fruitdrop_service_channel"
        private const val NOTIFICATION_ID = 1001

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        var instance: FruitDropOverlayService? = null
            private set
    }

    private lateinit var windowManager: WindowManager
    private var controlComposeView: ComposeView? = null
    private var markersComposeView: ComposeView? = null

    private var controlParams: WindowManager.LayoutParams? = null
    private var markersParams: WindowManager.LayoutParams? = null

    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private var screenWidth = 720
    private var screenHeight = 1600
    private var screenDensity = 320

    private var lastDropTime = 0L
    private var lastDetectedFruit: FruitType = FruitType.UNKNOWN
    private var isDropping = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        FruitDropController.init(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        lifecycleOwner = OverlayLifecycleOwner()

        val metrics = resources.displayMetrics
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        screenDensity = metrics.densityDpi

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        initOverlayViews()
        FruitDropController.setOverlayRunning(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val resultData: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }

        if (resultCode != 0 && resultData != null && mediaProjection == null) {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)
            setupVirtualDisplay()
        }

        startDetectionLoop()
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "FruitDrop Auto Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Layanan auto-drop FruitDrop berjalan di latar belakang"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("FruitDrop Auto Active")
            .setContentText("Memantau game Yeetalk secara real-time")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun initOverlayViews() {
        val owner = lifecycleOwner ?: return

        // 1. Fullscreen Markers View (KIRI / KANAN guide lines & circles)
        val mParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        markersParams = mParams

        val mComposeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setContent {
                MyApplicationTheme {
                    GuideMarkersOverlay()
                }
            }
        }
        markersComposeView = mComposeView
        try {
            windowManager.addView(mComposeView, mParams)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding markers view", e)
        }

        // 2. Control Panel / Minimized Bubble View
        val cParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 120
        }
        controlParams = cParams

        val cComposeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setContent {
                MyApplicationTheme {
                    val config by FruitDropController.config.collectAsState()

                    if (config.isMinimized) {
                        MinimizedBubbleOverlay(
                            onDrag = { dx, dy -> updateControlPosition(dx, dy) },
                            onClick = { FruitDropController.setMinimized(false) }
                        )
                    } else {
                        FloatingPanelOverlay(
                            onDrag = { dx, dy -> updateControlPosition(dx, dy) },
                            onClose = { stopSelf() }
                        )
                    }
                }
            }
        }
        controlComposeView = cComposeView
        try {
            windowManager.addView(cComposeView, cParams)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding control view", e)
        }
    }

    private fun updateControlPosition(dx: Float, dy: Float) {
        val params = controlParams ?: return
        val view = controlComposeView ?: return
        params.x = (params.x + dx.toInt()).coerceIn(0, screenWidth - 100)
        params.y = (params.y + dy.toInt()).coerceIn(40, screenHeight - 150)
        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating overlay layout", e)
        }
    }

    private fun setupVirtualDisplay() {
        val projection = mediaProjection ?: return
        val reader = ImageReader.newInstance(
            screenWidth,
            screenHeight,
            PixelFormat.RGBA_8888,
            2
        )
        imageReader = reader

        virtualDisplay = projection.createVirtualDisplay(
            "FruitDropScreen",
            screenWidth,
            screenHeight,
            screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,
            null,
            Handler(Looper.getMainLooper())
        )
    }

    private fun startDetectionLoop() {
        serviceScope.launch(Dispatchers.Default) {
            while (isActive) {
                try {
                    val config = FruitDropController.config.value
                    var capturedBitmap: Bitmap? = null

                    // Primary: Grab frame from ImageReader if MediaProjection is active
                    val reader = imageReader
                    if (reader != null) {
                        val image = reader.acquireLatestImage()
                        if (image != null) {
                            try {
                                val planes = image.planes
                                val buffer: ByteBuffer = planes[0].buffer
                                val pixelStride = planes[0].pixelStride
                                val rowStride = planes[0].rowStride
                                val rowPadding = rowStride - pixelStride * screenWidth

                                val bmp = Bitmap.createBitmap(
                                    screenWidth + rowPadding / pixelStride,
                                    screenHeight,
                                    Bitmap.Config.ARGB_8888
                                )
                                bmp.copyPixelsFromBuffer(buffer)
                                capturedBitmap = bmp
                            } catch (e: Exception) {
                                Log.e(TAG, "Error converting image to bitmap", e)
                            } finally {
                                image.close()
                            }
                        }
                    }

                    // Secondary fallback: AccessibilityService screenshot
                    if (capturedBitmap == null && FruitDropAccessibilityService.instance != null) {
                        FruitDropAccessibilityService.instance?.captureScreen { bmp ->
                            if (bmp != null) {
                                processBitmapAndDrop(bmp)
                            }
                        }
                    } else if (capturedBitmap != null) {
                        processBitmapAndDrop(capturedBitmap)
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Detection loop exception", e)
                }

                delay(75L) // Ultra fast 13+ checks per second
            }
        }
    }

    private fun processBitmapAndDrop(bitmap: Bitmap) {
        val config = FruitDropController.config.value
        val result = FruitDetector.detectFruit(bitmap, config)
        FruitDropController.updateDetection(result)

        if (!config.autoPlayEnabled) return
        if (result.fruit == FruitType.UNKNOWN) return

        val now = System.currentTimeMillis()
        val cooldown = config.dropDelayMs

        // Avoid dropping multiple times for the exact same frame occurrence
        if (now - lastDropTime < cooldown) return
        if (isDropping) return

        val accessibility = FruitDropAccessibilityService.instance ?: return

        val target = config.getTargetForFruit(result.fruit)
        if (target == DropTarget.NONE) return

        val targetX = when (target) {
            DropTarget.LEFT -> config.dropLeftRelX * screenWidth
            DropTarget.RIGHT -> config.dropRightRelX * screenWidth
            DropTarget.NONE -> return
        }
        val targetY = config.dropRelY * screenHeight

        isDropping = true
        accessibility.performClick(targetX, targetY) { success ->
            isDropping = false
            if (success) {
                lastDropTime = System.currentTimeMillis()
                lastDetectedFruit = result.fruit
                if (target == DropTarget.LEFT) {
                    FruitDropController.incrementLeftCount()
                } else {
                    FruitDropController.incrementRightCount()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        FruitDropController.setOverlayRunning(false)
        serviceScope.cancel()

        try {
            controlComposeView?.let { windowManager.removeView(it) }
            markersComposeView?.let { windowManager.removeView(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing overlay views", e)
        }

        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        lifecycleOwner?.destroy()
    }
}
