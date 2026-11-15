package com.example.controller

import android.content.Context
import android.content.SharedPreferences
import com.example.model.DetectionResult
import com.example.model.DropTarget
import com.example.model.FruitDropConfig
import com.example.model.FruitType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object FruitDropController {

    private const val PREFS_NAME = "fruit_drop_prefs"
    private var prefs: SharedPreferences? = null

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
        val scanRelX = p.getFloat("scanRelX", 0.085f)
        val scanRelY = p.getFloat("scanRelY", 0.281f)
        val scanRadius = p.getInt("scanRadiusDp", 24)
        val dropLeftRelX = p.getFloat("dropLeftRelX", 0.095f)
        val dropRightRelX = p.getFloat("dropRightRelX", 0.915f)
        val dropRelY = p.getFloat("dropRelY", 0.406f)
        val dropDelay = p.getLong("dropDelayMs", 250L)
        val showMarkers = p.getBoolean("showMarkers", true)
        val showScanBox = p.getBoolean("showScanBox", true)

        _config.update {
            it.copy(
                scanRelX = scanRelX,
                scanRelY = scanRelY,
                scanRadiusDp = scanRadius,
                dropLeftRelX = dropLeftRelX,
                dropRightRelX = dropRightRelX,
                dropRelY = dropRelY,
                dropDelayMs = dropDelay,
                showMarkers = showMarkers,
                showScanBox = showScanBox
            )
        }
    }

    private fun saveConfig() {
        val p = prefs?.edit() ?: return
        val current = _config.value
        p.putFloat("scanRelX", current.scanRelX)
        p.putFloat("scanRelY", current.scanRelY)
        p.putInt("scanRadiusDp", current.scanRadiusDp)
        p.putFloat("dropLeftRelX", current.dropLeftRelX)
        p.putFloat("dropRightRelX", current.dropRightRelX)
        p.putFloat("dropRelY", current.dropRelY)
        p.putLong("dropDelayMs", current.dropDelayMs)
        p.putBoolean("showMarkers", current.showMarkers)
        p.putBoolean("showScanBox", current.showScanBox)
        p.apply()
    }

    fun setAutoPlay(enabled: Boolean) {
        _config.update { it.copy(autoPlayEnabled = enabled) }
    }

    fun toggleAutoPlay(): Boolean {
        var newState = false
        _config.update {
            newState = !it.autoPlayEnabled
            it.copy(autoPlayEnabled = newState)
        }
        return newState
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
