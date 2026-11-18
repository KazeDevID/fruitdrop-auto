package com.mkz.ytfm.model

enum class ScanMode {
    NEXT_QUEUE, // Default: SCAN BUAH reads Next Fruit circle; waits for manual 1st drop, then shifts queue by 1
    DIRECT      // Direct: SCAN BUAH reads current active fruit under cloud and drops immediately
}

enum class QueueStage {
    IDLE,
    WAITING_FIRST_MANUAL,
    AUTO_RUNNING
}

data class FruitDropConfig(
    // Relative coordinates based on real screen dimensions (0.0f - 1.0f)
    val scanRelX: Float = 0.086f,      // ~62px on 720px wide screen (exact preview circle center)
    val scanRelY: Float = 0.281f,      // ~450px on 1600px high screen (exact preview circle center)
    val scanRadiusDp: Int = 13,        // ~26px on 720p — fits strictly inside preview fruit without hitting timer badge

    // Drop tap positions
    val dropLeftRelX: Float = 0.095f,  // Far left column (~68px on 720p)
    val dropRightRelX: Float = 0.915f, // Far right column (~660px on 720p)
    val dropRelY: Float = 0.406f,      // Near top of box (~650px on 1600p)

    // Performance & auto play
    val dropDelayMs: Long = 350L,      // Interval between actions in ms (allows Next preview animation to settle)
    val autoPlayEnabled: Boolean = false,
    val scanMode: ScanMode = ScanMode.NEXT_QUEUE,
    val queueStage: QueueStage = QueueStage.IDLE,
    val queuedFruit: FruitType = FruitType.UNKNOWN,
    val showMarkers: Boolean = true,   // "Penanda Titik: ON/OFF"
    val showScanBox: Boolean = true,   // Visual bounding circle for calibration
    val isCalibrating: Boolean = false, // Unlock dragging markers (disables touch pass-through)
    val isMinimized: Boolean = false,  // Minimized to small bubble mode

    // Drop rules per fruit
    val fruitRules: Map<String, DropTarget> = mapOf(
        FruitType.KIWI_HIJAU.id to DropTarget.LEFT,
        FruitType.PEACH.id to DropTarget.LEFT,
        FruitType.BLUEBERRY.id to DropTarget.RIGHT,
        FruitType.GRAPE.id to DropTarget.RIGHT,
        FruitType.STRAWBERRY.id to DropTarget.RIGHT,
        FruitType.ORANGE.id to DropTarget.RIGHT
    ),

    // Statistics
    val leftCount: Int = 0,
    val rightCount: Int = 0
) {
    fun getTargetForFruit(fruit: FruitType): DropTarget {
        return fruitRules[fruit.id] ?: fruit.defaultTarget
    }
}

data class DetectionResult(
    val fruit: FruitType,
    val target: DropTarget,
    val confidence: Float,
    val dominantHue: Float,
    val dominantSat: Float,
    val dominantVal: Float,
    val timestamp: Long = System.currentTimeMillis()
)
