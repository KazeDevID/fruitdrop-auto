package com.example.model

data class FruitDropConfig(
    // Relative coordinates based on screen dimensions (0.0f - 1.0f)
    val scanRelX: Float = 0.085f,      // ~61px on 720px wide screen
    val scanRelY: Float = 0.281f,      // ~450px on 1600px high screen
    val scanRadiusDp: Int = 24,

    // Drop tap positions
    val dropLeftRelX: Float = 0.095f,  // Far left column (~68px on 720p)
    val dropRightRelX: Float = 0.915f, // Far right column (~660px on 720p)
    val dropRelY: Float = 0.406f,      // Near top of box (~650px on 1600p)

    // Performance & auto play
    val dropDelayMs: Long = 250L,      // Interval between actions in ms
    val autoPlayEnabled: Boolean = false,
    val showMarkers: Boolean = true,   // "Penanda Titik: ON/OFF"
    val showScanBox: Boolean = true,   // Visual bounding circle for calibration
    val isMinimized: Boolean = false,  // Minimized to small bubble mode

    // Drop rules per fruit
    val fruitRules: Map<String, DropTarget> = mapOf(
        FruitType.KIWI_HIJAU.id to DropTarget.LEFT,
        FruitType.PEACH.id to DropTarget.LEFT,
        FruitType.BLUEBERRY.id to DropTarget.RIGHT,
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
