package com.example.model

import androidx.compose.ui.graphics.Color

enum class DropTarget {
    LEFT,
    RIGHT,
    NONE
}

enum class FruitType(
    val id: String,
    val displayName: String,
    val defaultTarget: DropTarget,
    val badgeColor: Long,
    val isMarkedByUser: Boolean,
    val description: String
) {
    KIWI_HIJAU(
        id = "kiwi_hijau",
        displayName = "Kiwi Hijau",
        defaultTarget = DropTarget.LEFT,
        badgeColor = 0xFF76BA1B,
        isMarkedByUser = true,
        description = "Ditandai user: Drop paling kiri (hijau cerah berbiji hitam)"
    ),
    PEACH(
        id = "peach",
        displayName = "Buah Persik (Peach)",
        defaultTarget = DropTarget.LEFT,
        badgeColor = 0xFFFF9E80,
        isMarkedByUser = true,
        description = "Ditandai user: Drop paling kiri (warna peach/pastel berdaun)"
    ),
    BLUEBERRY(
        id = "blueberry",
        displayName = "Blueberry",
        defaultTarget = DropTarget.RIGHT,
        badgeColor = 0xFF5364DE,
        isMarkedByUser = false,
        description = "Kecil dari kiwi: Drop paling kanan (biru keunguan)"
    ),
    GRAPE(
        id = "grape",
        displayName = "Anggur Ungu",
        defaultTarget = DropTarget.RIGHT,
        badgeColor = 0xFFAB47BC,
        isMarkedByUser = false,
        description = "Kecil dari kiwi: Drop paling kanan (ungu magenta)"
    ),
    STRAWBERRY(
        id = "strawberry",
        displayName = "Stroberi / Cherry",
        defaultTarget = DropTarget.RIGHT,
        badgeColor = 0xFFE53935,
        isMarkedByUser = false,
        description = "Kecil dari kiwi: Drop paling kanan (merah cerah)"
    ),
    ORANGE(
        id = "orange",
        displayName = "Jeruk / Lemon",
        defaultTarget = DropTarget.RIGHT,
        badgeColor = 0xFFFB8C00,
        isMarkedByUser = false,
        description = "Kecil dari kiwi: Drop paling kanan (oranye/kuning)"
    ),
    UNKNOWN(
        id = "unknown",
        displayName = "Memindai Buah...",
        defaultTarget = DropTarget.NONE,
        badgeColor = 0xFF78909C,
        isMarkedByUser = false,
        description = "Belum terdeteksi / transisi buah"
    );

    val composeColor: Color get() = Color(badgeColor)
}
