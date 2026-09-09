package com.example.data.model

data class Token(
    val id: Int, // 0..3
    val color: LudoColor,
    val stepCount: Int = -1 // -1 = Yard, 0 = Start, 1..50 = Common Track, 51..55 = Home Stretch, 56 = Home
) {
    val isInYard: Boolean get() = stepCount == -1
    val isHome: Boolean get() = stepCount == 56
    val isOnBoard: Boolean get() = stepCount in 0..55
    val isOnHomeStretch: Boolean get() = stepCount in 51..55

    val globalTrackIndex: Int
        get() = if (stepCount in 0..50) {
            (color.startTrackIndex + stepCount) % 52
        } else {
            -1
        }

    fun canMove(diceRoll: Int): Boolean {
        if (isHome) return false
        if (isInYard) return diceRoll == 6
        return (stepCount + diceRoll) <= 56
    }
}
