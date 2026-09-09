package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.LudoColor
import com.example.data.model.LudoDiceState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Configuration for a player in the dice and turn management system.
 */
data class PlayerDiceConfig(
    val name: String,
    val color: LudoColor,
    val isBot: Boolean = false
)

/**
 * ViewModel managing the Ludo dice lifecycle, fair random number generation,
 * rolling animations, and turn progression according to official Ludo rules.
 */
class LudoDiceViewModel(
    private val randomSource: Random = Random.Default,
    private val externalScope: kotlinx.coroutines.CoroutineScope? = null
) : ViewModel() {

    private val scope: kotlinx.coroutines.CoroutineScope
        get() = externalScope ?: viewModelScope

    private val defaultPlayers = listOf(
        PlayerDiceConfig("Player 1", LudoColor.RED, isBot = false),
        PlayerDiceConfig("Player 2", LudoColor.GREEN, isBot = true),
        PlayerDiceConfig("Player 3", LudoColor.YELLOW, isBot = true),
        PlayerDiceConfig("Player 4", LudoColor.BLUE, isBot = true)
    )

    private var players: List<PlayerDiceConfig> = defaultPlayers

    private val _diceState = MutableStateFlow(
        LudoDiceState(
            value = 1,
            isRolling = false,
            hasRolled = false,
            activePlayerIndex = 0,
            activePlayerColor = defaultPlayers[0].color,
            activePlayerName = defaultPlayers[0].name,
            consecutiveSixes = 0,
            turnTimeRemaining = DEFAULT_TURN_DURATION_SECONDS,
            message = "${defaultPlayers[0].name}'s turn to roll!"
        )
    )
    val diceState: StateFlow<LudoDiceState> = _diceState.asStateFlow()

    private var turnTimerJob: Job? = null
    private var rollJob: Job? = null

    init {
        startTurnTimer()
    }

    /**
     * Initializes or updates the list of participating players and resets turn state.
     */
    fun setupPlayers(playerList: List<PlayerDiceConfig>) {
        if (playerList.isEmpty()) return
        players = playerList
        turnTimerJob?.cancel()
        rollJob?.cancel()

        val firstPlayer = playerList[0]
        _diceState.value = LudoDiceState(
            value = 1,
            isRolling = false,
            hasRolled = false,
            activePlayerIndex = 0,
            activePlayerColor = firstPlayer.color,
            activePlayerName = firstPlayer.name,
            consecutiveSixes = 0,
            turnTimeRemaining = DEFAULT_TURN_DURATION_SECONDS,
            message = "${firstPlayer.name}'s turn to roll!"
        )
        startTurnTimer()
    }

    /**
     * Initiates the dice roll with random number generation and animation frames.
     *
     * @param onRollComplete Optional callback invoked when the final roll value is determined.
     */
    fun rollDice(onRollComplete: ((Int) -> Unit)? = null) {
        val currentState = _diceState.value
        if (!currentState.canRoll) return

        turnTimerJob?.cancel()

        rollJob?.cancel()
        rollJob = scope.launch {
            _diceState.update {
                it.copy(
                    isRolling = true,
                    message = "${it.activePlayerName} is rolling..."
                )
            }

            // Rapidly fluctuate values for visual rolling animation (7 frames)
            repeat(ROLL_ANIMATION_FRAMES) {
                delay(ROLL_FRAME_INTERVAL_MS)
                val intermediateValue = randomSource.nextInt(1, 7)
                _diceState.update { it.copy(value = intermediateValue) }
            }

            // Generate final fair random roll between 1 and 6 (inclusive)
            val finalRoll = randomSource.nextInt(1, 7)
            val currentConsecutive = if (finalRoll == 6) {
                currentState.consecutiveSixes + 1
            } else {
                0
            }

            val updatedHistory = listOf(finalRoll) + currentState.rollHistory.take(19)

            if (currentConsecutive >= 3) {
                // Rule: Three consecutive sixes forfeits the turn
                _diceState.update {
                    it.copy(
                        value = finalRoll,
                        isRolling = false,
                        hasRolled = true,
                        consecutiveSixes = currentConsecutive,
                        isThreeSixesPenalty = true,
                        isBonusRoll = false,
                        rollHistory = updatedHistory,
                        message = "Three 6s in a row! ${it.activePlayerName} forfeits turn."
                    )
                }

                delay(1200L)
                passTurn(reason = "Three consecutive sixes")
                onRollComplete?.invoke(finalRoll)
                return@launch
            }

            val earnedBonus = (finalRoll == 6)
            val eventMsg = if (earnedBonus) {
                "${currentState.activePlayerName} rolled a 6! Extra roll awarded."
            } else {
                "${currentState.activePlayerName} rolled a $finalRoll."
            }

            _diceState.update {
                it.copy(
                    value = finalRoll,
                    isRolling = false,
                    hasRolled = true,
                    consecutiveSixes = currentConsecutive,
                    isThreeSixesPenalty = false,
                    isBonusRoll = earnedBonus,
                    rollHistory = updatedHistory,
                    message = eventMsg
                )
            }

            // Start post-roll decision timer for moving tokens
            startTurnTimer()

            onRollComplete?.invoke(finalRoll)
        }
    }

    /**
     * Passes the turn to the next player in the sequence.
     */
    fun passTurn(reason: String = "") {
        turnTimerJob?.cancel()
        rollJob?.cancel()

        val nextIndex = (_diceState.value.activePlayerIndex + 1) % players.size
        val nextPlayer = players[nextIndex]

        val reasonText = if (reason.isNotBlank()) " ($reason)" else ""

        _diceState.update { current ->
            current.copy(
                isRolling = false,
                hasRolled = false,
                activePlayerIndex = nextIndex,
                activePlayerColor = nextPlayer.color,
                activePlayerName = nextPlayer.name,
                consecutiveSixes = 0,
                isThreeSixesPenalty = false,
                isBonusRoll = false,
                turnTimeRemaining = DEFAULT_TURN_DURATION_SECONDS,
                isTurnTimeExpired = false,
                totalTurnsCompleted = current.totalTurnsCompleted + 1,
                message = "${nextPlayer.name}'s turn$reasonText"
            )
        }

        startTurnTimer()
    }

    /**
     * Grants a bonus turn to the current active player (e.g. after rolling a 6, capturing, or reaching home).
     */
    fun grantBonusTurn(reason: String = "Bonus turn granted!") {
        turnTimerJob?.cancel()
        rollJob?.cancel()

        _diceState.update { current ->
            current.copy(
                isRolling = false,
                hasRolled = false,
                isBonusRoll = true,
                turnTimeRemaining = DEFAULT_TURN_DURATION_SECONDS,
                isTurnTimeExpired = false,
                message = "${current.activePlayerName}: $reason Roll again!"
            )
        }

        startTurnTimer()
    }

    /**
     * Handles turn completion after a token movement.
     *
     * @param hasAdditionalBonus If the token move triggered a capture or reached home, granting an additional roll.
     */
    fun onMoveCompleted(hasAdditionalBonus: Boolean = false) {
        val state = _diceState.value
        if (hasAdditionalBonus || (state.isBonusRoll && state.consecutiveSixes < 3)) {
            grantBonusTurn(
                if (hasAdditionalBonus) "Token captured/reached home!" else "Rolled a 6!"
            )
        } else {
            passTurn()
        }
    }

    /**
     * Starts the countdown timer for the active player's turn.
     */
    private fun startTurnTimer() {
        turnTimerJob?.cancel()
        turnTimerJob = scope.launch {
            for (seconds in DEFAULT_TURN_DURATION_SECONDS downTo 0) {
                _diceState.update { it.copy(turnTimeRemaining = seconds) }
                if (seconds > 0) {
                    delay(1000L)
                }
            }

            // Timer expired: auto-forfeit / pass turn
            val currentState = _diceState.value
            _diceState.update {
                it.copy(
                    isTurnTimeExpired = true,
                    message = "${it.activePlayerName}'s time ran out!"
                )
            }
            delay(600L)
            passTurn(reason = "Time expired")
        }
    }

    /**
     * Resets the entire game turn counter and dice state.
     */
    fun resetGame() {
        setupPlayers(players)
    }

    override fun onCleared() {
        super.onCleared()
        turnTimerJob?.cancel()
        rollJob?.cancel()
    }

    companion object {
        const val DEFAULT_TURN_DURATION_SECONDS = 15
        const val ROLL_ANIMATION_FRAMES = 7
        const val ROLL_FRAME_INTERVAL_MS = 50L
    }
}
