package com.example.data.model

/**
 * Data class representing the state of the Ludo dice and the active turn.
 *
 * @property value Current displayed face value of the dice (1 to 6).
 * @property isRolling Indicates if the dice is currently in the rolling animation.
 * @property hasRolled Indicates if the dice has been rolled for the current turn.
 * @property activePlayerIndex Zero-based index of the player whose turn it is.
 * @property activePlayerColor Color of the active player (RED, GREEN, YELLOW, BLUE).
 * @property activePlayerName Display name of the active player.
 * @property consecutiveSixes Count of consecutive 6s rolled in this turn sequence (0, 1, 2, or 3).
 * @property isThreeSixesPenalty Indicates if the turn is forfeited due to rolling three consecutive 6s.
 * @property isBonusRoll Indicates if the current roll qualifies for an extra/bonus turn.
 * @property turnTimeRemaining Countdown timer in seconds for the active player's turn.
 * @property isTurnTimeExpired Indicates if the turn timer has expired for the active player.
 * @property rollHistory Recent rolls in chronological or reverse-chronological order.
 * @property totalTurnsCompleted Total count of turns completed in the game so far.
 * @property message Status or event message explaining the current dice and turn state.
 */
data class LudoDiceState(
    val value: Int = 1,
    val isRolling: Boolean = false,
    val hasRolled: Boolean = false,
    val activePlayerIndex: Int = 0,
    val activePlayerColor: LudoColor = LudoColor.RED,
    val activePlayerName: String = "Player 1",
    val consecutiveSixes: Int = 0,
    val isThreeSixesPenalty: Boolean = false,
    val isBonusRoll: Boolean = false,
    val turnTimeRemaining: Int = 15,
    val isTurnTimeExpired: Boolean = false,
    val rollHistory: List<Int> = emptyList(),
    val totalTurnsCompleted: Int = 0,
    val message: String = "Tap dice to roll!"
) {
    /**
     * Whether the active player can interactively roll the dice right now.
     */
    val canRoll: Boolean
        get() = !isRolling && !hasRolled && !isThreeSixesPenalty && !isTurnTimeExpired

    /**
     * Whether the current dice value is a 6.
     */
    val isSix: Boolean
        get() = value == 6

    /**
     * Whether this roll qualifies for a bonus roll (rolled a 6 and not forfeited).
     */
    val qualifiesForBonusRoll: Boolean
        get() = isSix && consecutiveSixes < 3
}
