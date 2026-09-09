package com.example.data.model

import androidx.compose.ui.graphics.Color

enum class LudoColor(
    val displayName: String,
    val primaryColor: Color,
    val lightColor: Color,
    val darkColor: Color,
    val startTrackIndex: Int, // Index on 52-step global track
    val homeTrackEntranceStep: Int = 50 // When token has moved 51 steps relative to start
) {
    RED(
        displayName = "Red",
        primaryColor = Color(0xFFE53935),
        lightColor = Color(0xFFFFCDD2),
        darkColor = Color(0xFFB71C1C),
        startTrackIndex = 0
    ),
    GREEN(
        displayName = "Green",
        primaryColor = Color(0xFF2E7D32),
        lightColor = Color(0xFFC8E6C9),
        darkColor = Color(0xFF1B5E20),
        startTrackIndex = 13
    ),
    YELLOW(
        displayName = "Yellow",
        primaryColor = Color(0xFFF9A825),
        lightColor = Color(0xFFFFF9C4),
        darkColor = Color(0xFFF57F17),
        startTrackIndex = 26
    ),
    BLUE(
        displayName = "Blue",
        primaryColor = Color(0xFF1976D2),
        lightColor = Color(0xFFBBDEFB),
        darkColor = Color(0xFF0D47A1),
        startTrackIndex = 39
    );

    val next: LudoColor
        get() = when (this) {
            RED -> GREEN
            GREEN -> YELLOW
            YELLOW -> BLUE
            BLUE -> RED
        }
}
