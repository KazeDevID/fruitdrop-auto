package com.example.controller

import android.content.Context
import android.content.SharedPreferences
import com.example.model.DetectionResult
import com.example.model.DropTarget
import com.example.model.FruitDropConfig
import com.example.model.FruitType
import com.example.model.QueueStage
import com.example.model.ScanMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicBoolean

object FruitDropController {

    private const val PREFS_NAME = "fruit_drop_prefs"
    private const val CURRENT_PREFS_VERSION = 4
    private var prefs: SharedPreferences? = null
    private val manualDropConfirmedFlag = AtomicBoolean(false)

    private val _config = MutableStateFlow(FruitDropConfig())
    val config: StateFlow<FruitDropConfig> = _config.asStateFlow()

    private val _currentDetection = MutableStateFlow(
        DetectionResult(
            fruit = FruitType.UNKNOWN,
            target = DropTarget.NONE,
            confidence = 0f,
            dominantHue = 0f,
            dominantSat = 0f,
            dominantVal = 0f
        )
    )
    val currentDetection: StateFlow<DetectionResult> = _currentDetection.asStateFlow()

    private val _isAccessibilityConnected = MutableStateFlow(false)
    val isAccessibilityConnected: StateFlow<Boolean> = _isAccessibilityConnected.asStateFlow()

    private val _isOverlayRunning = MutableStateFlow(false)
    val isOverlayRunning: StateFlow<Boolean> = _isOverlayRunning.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadConfig()
        }
    }

    private fun loadConfig() {
        val p = prefs ?: return
        val prefsVersion = p.getInt("prefsVersion", 0)
        var scanRelX = p.getFloat("scanRelX", 0.086f)
        var scanRelY = p.getFloat("scanRelY", 0.281f)
        var scanRadius = p.getInt("scanRadiusDp", 13)
        val dropLeftRelX = p.getFloat("dropLeftRelX", 0.095f)
        val dropRightRelX = p.getFloat("dropRightRelX", 0.915f)
        val dropRelY = p.getFloat("dropRelY", 0.406f)
        var dropDelay = p.getLong("dropDelayMs", 350L)
        val showMarkers = p.getBoolean("showMarkers", true)
        val showScanBox = p.getBoolean("showScanBox", true)
        val scanModeName = p.getString("scanMode", ScanMode.NEXT_QUEUE.name)
        val scanMode = runCatching { ScanMode.valueOf(scanModeName ?: ScanMode.NEXT_QUEUE.name) }
            .getOrDefault(ScanMode.NEXT_QUEUE)

        // Migrate legacy v1.0.0–v1.0.3 scan defaults cleanly
        if (prefsVersion < CURRENT_PREFS_VERSION) {
            if (scanRadius >= 22) {
                scanRadius = 13
            }
            if (kotlin.math.abs(scanRelX - 0.085f) < 0.002f) {
                scanRelX = 0.086f
            }
            if (kotlin.math.abs(scanRelY - 0.281f) < 0.002f) {
                scanRelY = 0.281f
            }
            if (prefsVersion < 4 && dropDelay == 250L) {
                dropDelay = 350L
            }
        }

        _config.update {
            it.copy(
                scanRelX = scanRelX,
                scanRelY = scanRelY,
                scanRadiusDp = scanRadius,
                dropLeftRelX = dropLeftRelX,
                dropRightRelX = dropRightRelX,
                dropRelY = dropRelY,
                dropDelayMs = dropDelay,
                scanMode = scanMode,
                showMarkers = showMarkers,
                showScanBox = showScanBox
            )
        }

        if (prefsVersion < CURRENT_PREFS_VERSION) {
            saveConfig()
        }
    }

    private fun saveConfig() {
        val p = prefs?.edit() ?: return
        val current = _config.value
        p.putInt("prefsVersion", CURRENT_PREFS_VERSION)
        p.putFloat("scanRelX", current.scanRelX)
        p.putFloat("scanRelY", current.scanRelY)
        p.putInt("scanRadiusDp", current.scanRadiusDp)
        p.putFloat("dropLeftRelX", current.dropLeftRelX)
        p.putFloat("dropRightRelX", current.dropRightRelX)
        p.putFloat("dropRelY", current.dropRelY)
        p.putLong("dropDelayMs", current.dropDelayMs)
        p.putString("scanMode", current.scanMode.name)
        p.putBoolean("showMarkers", current.showMarkers)
        p.putBoolean("showScanBox", current.showScanBox)
        p.apply()
    }

    fun setAutoPlay(enabled: Boolean) {
        manualDropConfirmedFlag.set(false)
        _config.update {
            val nextStage = when {
                !enabled -> QueueStage.IDLE
                it.scanMode == ScanMode.NEXT_QUEUE -> QueueStage.WAITING_FIRST_MANUAL
                else -> QueueStage.AUTO_RUNNING
            }
            it.copy(
                autoPlayEnabled = enabled,
                queueStage = nextStage,
                queuedFruit = FruitType.UNKNOWN
            )
        }
    }

    fun toggleAutoPlay(): Boolean {
        var newState = false
        manualDropConfirmedFlag.set(false)
        _config.update {
            newState = !it.autoPlayEnabled
            val nextStage = when {
                !newState -> QueueStage.IDLE
                it.scanMode == ScanMode.NEXT_QUEUE -> QueueStage.WAITING_FIRST_MANUAL
                else -> QueueStage.AUTO_RUNNING
            }
            it.copy(
                autoPlayEnabled = newState,
                queueStage = nextStage,
                queuedFruit = FruitType.UNKNOWN
            )
        }
        return newState
    }

    fun toggleScanMode(): ScanMode {
        var updatedMode = ScanMode.NEXT_QUEUE
        manualDropConfirmedFlag.set(false)
        _config.update {
            updatedMode = if (it.scanMode == ScanMode.NEXT_QUEUE) ScanMode.DIRECT else ScanMode.NEXT_QUEUE
            val nextStage = when {
                !it.autoPlayEnabled -> QueueStage.IDLE
                updatedMode == ScanMode.NEXT_QUEUE -> QueueStage.WAITING_FIRST_MANUAL
                else -> QueueStage.AUTO_RUNNING
            }
            it.copy(
                scanMode = updatedMode,
                queueStage = nextStage,
                queuedFruit = FruitType.UNKNOWN
            )
        }
        saveConfig()
        return updatedMode
    }

    fun setQueueState(stage: QueueStage, queuedFruit: FruitType) {
        _config.update {
            if (it.queueStage == stage && it.queuedFruit == queuedFruit) it
            else it.copy(queueStage = stage, queuedFruit = queuedFruit)
        }
    }

    fun confirmFirstManualDrop() {
        manualDropConfirmedFlag.set(true)
    }

    fun consumeManualDropConfirmation(): Boolean {
        return manualDropConfirmedFlag.getAndSet(false)
    }

    fun toggleMarkers() {
        _config.update {
            val updated = it.copy(showMarkers = !it.showMarkers)
            saveConfig()
            updated
        }
    }

    fun toggleScanBox() {
        _config.update {
            val updated = it.copy(showScanBox = !it.showScanBox)
            saveConfig()
            updated
        }
    }

    fun toggleCalibrating() {
        _config.update {
            val next = !it.isCalibrating
            it.copy(
                isCalibrating = next,
                showMarkers = if (next) true else it.showMarkers,
                showScanBox = if (next) true else it.showScanBox
            )
        }
    }

    fun setMinimized(minimized: Boolean) {
        _config.update { it.copy(isMinimized = minimized) }
    }

    fun updateCoordinates(
        scanRelX: Float? = null,
        scanRelY: Float? = null,
        dropLeftRelX: Float? = null,
        dropRightRelX: Float? = null,
        dropRelY: Float? = null
    ) {
        _config.update {
            it.copy(
                scanRelX = scanRelX ?: it.scanRelX,
                scanRelY = scanRelY ?: it.scanRelY,
                dropLeftRelX = dropLeftRelX ?: it.dropLeftRelX,
                dropRightRelX = dropRightRelX ?: it.dropRightRelX,
                dropRelY = dropRelY ?: it.dropRelY
            )
        }
        saveConfig()
    }

    fun setDropDelay(delayMs: Long) {
        _config.update { it.copy(dropDelayMs = delayMs) }
        saveConfig()
    }

    fun setScanRadiusDp(radiusDp: Int) {
        _config.update { it.copy(scanRadiusDp = radiusDp.coerceIn(8, 30)) }
        saveConfig()
    }

    fun setFruitRule(fruitId: String, target: DropTarget) {
        _config.update {
            val updatedRules = it.fruitRules.toMutableMap()
            updatedRules[fruitId] = target
            it.copy(fruitRules = updatedRules)
        }
    }

    fun incrementLeftCount() {
        _config.update { it.copy(leftCount = it.leftCount + 1) }
    }

    fun incrementRightCount() {
        _config.update { it.copy(rightCount = it.rightCount + 1) }
    }

    fun resetCounters() {
        _config.update { it.copy(leftCount = 0, rightCount = 0) }
    }

    fun updateDetection(result: DetectionResult) {
        _currentDetection.value = result
    }

    fun setAccessibilityConnected(connected: Boolean) {
        _isAccessibilityConnected.value = connected
    }

    fun setOverlayRunning(running: Boolean) {
        _isOverlayRunning.value = running
    }
}
