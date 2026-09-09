package com.example.engine

import com.example.audio.SoundManager
import com.example.data.model.ChatMessage
import com.example.data.model.FloatingEmoji
import com.example.data.model.GameMode
import com.example.data.model.GameState
import com.example.data.model.LudoColor
import com.example.data.model.Player
import com.example.data.model.PowerUpType
import com.example.data.model.Token
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class LudoGameEngine(
    private val soundManager: SoundManager,
    private val scope: CoroutineScope
) {
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private var turnTimerJob: Job? = null
    private var botTurnJob: Job? = null

    // Tactical power-ups state
    private var forceNextRollSix = false
    private var nextMoveExtraSteps = 0
    private val shieldedTokens = mutableSetOf<String>()
    private val frozenPlayers = mutableSetOf<LudoColor>()

    fun applyPowerUp(type: PowerUpType, playerColor: LudoColor): Boolean {
        when (type) {
            PowerUpType.GOLDEN_DICE -> {
                forceNextRollSix = true
                _gameState.value = _gameState.value.copy(
                    eventMessage = "🎲 Lucky Sixer Activated! Next roll is guaranteed a 6!"
                )
                return true
            }
            PowerUpType.DOUBLE_BOOST -> {
                nextMoveExtraSteps = 2
                _gameState.value = _gameState.value.copy(
                    eventMessage = "⚡ Speed Boost Activated! Next move jumps +2 bonus steps!"
                )
                return true
            }
            PowerUpType.DIVINE_SHIELD -> {
                val player = _gameState.value.players.find { it.color == playerColor }
                val leadToken = player?.tokens?.filter { it.isOnBoard }?.maxByOrNull { it.stepCount }
                val tokenKey = if (leadToken != null) "${playerColor}_${leadToken.id}" else "${playerColor}_0"
                shieldedTokens.add(tokenKey)
                _gameState.value = _gameState.value.copy(
                    eventMessage = "🛡️ Divine Shield Activated on your token!"
                )
                return true
            }
            PowerUpType.FREEZE_SPELL -> {
                val opponents = _gameState.value.players.filter { it.color != playerColor && !it.hasWon }
                val targetOpp = opponents.firstOrNull()
                if (targetOpp != null) {
                    frozenPlayers.add(targetOpp.color)
                    _gameState.value = _gameState.value.copy(
                        eventMessage = "❄️ Freeze Spell cast on ${targetOpp.name}! Turn will be skipped!"
                    )
                    return true
                }
                return false
            }
        }
    }

    fun startNewGame(
        players: List<Player>,
        gameMode: GameMode,
        roomCode: String = (100000..999999).random().toString()
    ) {
        turnTimerJob?.cancel()
        botTurnJob?.cancel()

        _gameState.value = GameState(
            gameMode = gameMode,
            roomCode = roomCode,
            players = players,
            activePlayerIndex = 0,
            diceRoll = 0,
            isRolling = false,
            hasRolled = false,
            consecutiveSixes = 0,
            turnTimeRemaining = 15,
            isGameOver = false,
            winner = null,
            rankings = emptyList(),
            eventMessage = "Game started! ${players[0].name}'s turn."
        )

        startTurnTimer()
        checkBotTurn()
    }

    private fun startTurnTimer() {
        turnTimerJob?.cancel()
        turnTimerJob = scope.launch(Dispatchers.Default) {
            for (timeLeft in 15 downTo 0) {
                _gameState.value = _gameState.value.copy(turnTimeRemaining = timeLeft)
                delay(1000L)
            }
            // Timer expired: auto-skip turn
            val currentState = _gameState.value
            if (!currentState.isGameOver) {
                _gameState.value = currentState.copy(
                    eventMessage = "${currentState.activePlayer?.name ?: "Player"}'s time ran out!"
                )
                passTurnToNextPlayer(reason = "Turn timed out")
            }
        }
    }

    fun rollDice() {
        val state = _gameState.value
        if (state.isRolling || state.hasRolled || state.isGameOver) return

        turnTimerJob?.cancel()
        soundManager.playDiceRoll()

        scope.launch(Dispatchers.Default) {
            _gameState.value = state.copy(isRolling = true)

            // Animated rolling fluctuation
            for (i in 1..8) {
                delay(50L)
                _gameState.value = _gameState.value.copy(diceRoll = Random.nextInt(1, 7))
            }

            // Final determined dice roll
            val finalRoll = if (forceNextRollSix) {
                forceNextRollSix = false
                6
            } else {
                Random.nextInt(1, 7)
            }
            val consecutive = if (finalRoll == 6) state.consecutiveSixes + 1 else 0

            _gameState.value = _gameState.value.copy(
                isRolling = false,
                hasRolled = true,
                diceRoll = finalRoll,
                consecutiveSixes = consecutive
            )

            // Rule: Three consecutive 6s forfeits turn
            if (consecutive >= 3) {
                _gameState.value = _gameState.value.copy(
                    eventMessage = "Three 6s in a row! Turn forfeited."
                )
                delay(800L)
                passTurnToNextPlayer("Three consecutive sixes")
                return@launch
            }

            // Check if player has any valid moves
            val activePlayer = _gameState.value.activePlayer ?: return@launch
            val validMoves = activePlayer.tokens.filter { it.canMove(finalRoll) }

            if (validMoves.isEmpty()) {
                _gameState.value = _gameState.value.copy(
                    eventMessage = "No moves possible for ${activePlayer.name} with roll of $finalRoll."
                )
                delay(1000L)
                passTurnToNextPlayer("No valid moves")
            } else if (validMoves.size == 1 && (activePlayer.isBot || finalRoll == 6 && validMoves[0].isInYard)) {
                // Auto-execute if only single obvious move
                delay(400L)
                executeMove(validMoves.first().id)
            } else {
                // Start a decision countdown timer
                startTurnTimer()
            }
        }
    }

    fun executeMove(tokenId: Int) {
        val state = _gameState.value
        if (!state.hasRolled || state.isRolling || state.isGameOver) return

        val activePlayer = state.activePlayer ?: return
        val token = activePlayer.tokens.find { it.id == tokenId } ?: return
        val diceRoll = state.diceRoll

        if (!token.canMove(diceRoll)) return

        turnTimerJob?.cancel()

        scope.launch(Dispatchers.Default) {
            // Animate token moving
            _gameState.value = _gameState.value.copy(movingTokenId = tokenId)

            val currentStep = token.stepCount
            val extraSteps = if (currentStep != -1) nextMoveExtraSteps else 0
            nextMoveExtraSteps = 0
            val destStep = if (currentStep == -1) 0 else (currentStep + diceRoll + extraSteps).coerceAtMost(56)

            // Step-by-step sound and movement animation
            if (currentStep == -1) {
                // Moving out of yard
                soundManager.playTokenMove()
                delay(200L)
            } else {
                for (step in (currentStep + 1)..destStep) {
                    val tempPlayer = activePlayer.updateToken(tokenId, step)
                    val updatedPlayers = state.players.map { if (it.color == activePlayer.color) tempPlayer else it }
                    _gameState.value = _gameState.value.copy(players = updatedPlayers)
                    soundManager.playTokenMove()
                    delay(120L)
                }
            }

            // Finalize destination step
            var updatedPlayer = activePlayer.updateToken(tokenId, destStep)
            var earnedExtraTurn = (diceRoll == 6)
            var eventMsg = "${activePlayer.name} moved a token by $diceRoll."

            // Check if token reached Home (56)
            if (destStep == 56) {
                soundManager.playTokenHome()
                earnedExtraTurn = true
                eventMsg = "🎉 ${activePlayer.name}'s token reached HOME! Extra roll!"
            }

            // Check if token captured any opponent tokens
            var allUpdatedPlayers = state.players.map { if (it.color == activePlayer.color) updatedPlayer else it }

            if (destStep in 0..50) {
                val destGlobalIdx = (activePlayer.color.startTrackIndex + destStep) % 52
                val isSafe = LudoBoardCoordinates.isSafeCell(destGlobalIdx)

                if (!isSafe) {
                    var capturedAny = false
                    allUpdatedPlayers = allUpdatedPlayers.map { opponent ->
                        if (opponent.color != activePlayer.color) {
                            val capturedTokens = opponent.tokens.map { oppToken ->
                                if (oppToken.globalTrackIndex == destGlobalIdx) {
                                    val shieldKey = "${opponent.color}_${oppToken.id}"
                                    if (shieldedTokens.contains(shieldKey)) {
                                        shieldedTokens.remove(shieldKey)
                                        eventMsg = "🛡️ ${opponent.name}'s Divine Shield deflected the attack!"
                                        oppToken
                                    } else {
                                        capturedAny = true
                                        oppToken.copy(stepCount = -1) // Send back to yard!
                                    }
                                } else {
                                    oppToken
                                }
                            }
                            opponent.copy(tokens = capturedTokens)
                        } else {
                            opponent
                        }
                    }

                    if (capturedAny) {
                        soundManager.playTokenCapture()
                        earnedExtraTurn = true
                        eventMsg = "⚔️ ${activePlayer.name} captured an opponent's token! Extra turn!"
                    }
                }
            }

            // Check win condition for active player
            val playerWon = updatedPlayer.tokens.all { it.isHome }
            var isGameOver = false
            var winner: Player? = null
            val rankings = state.rankings.toMutableList()

            if (playerWon && !rankings.any { it.color == activePlayer.color }) {
                updatedPlayer = updatedPlayer.copy(finishedRank = rankings.size + 1)
                rankings.add(updatedPlayer)
                allUpdatedPlayers = allUpdatedPlayers.map { if (it.color == activePlayer.color) updatedPlayer else it }
                soundManager.playVictory()

                if (rankings.size == 1) {
                    winner = updatedPlayer
                    eventMsg = "🏆 👑 ${updatedPlayer.name} WINS THE GAME! 👑 🏆"
                    isGameOver = true
                }
            }

            _gameState.value = _gameState.value.copy(
                players = allUpdatedPlayers,
                movingTokenId = null,
                hasRolled = false,
                diceRoll = 0,
                eventMessage = eventMsg,
                isGameOver = isGameOver,
                winner = winner,
                rankings = rankings
            )

            if (!isGameOver) {
                if (earnedExtraTurn) {
                    // Active player gets another roll!
                    delay(500L)
                    startTurnTimer()
                    checkBotTurn()
                } else {
                    passTurnToNextPlayer("Turn completed")
                }
            }
        }
    }

    private fun passTurnToNextPlayer(reason: String) {
        val state = _gameState.value
        if (state.isGameOver) return

        var nextIndex = (state.activePlayerIndex + 1) % state.players.size
        var attempts = 0
        // Skip players who have already won/finished all tokens
        while (state.players[nextIndex].hasWon && attempts < state.players.size) {
            nextIndex = (nextIndex + 1) % state.players.size
            attempts++
        }

        val nextPlayer = state.players[nextIndex]

        if (frozenPlayers.contains(nextPlayer.color)) {
            frozenPlayers.remove(nextPlayer.color)
            _gameState.value = state.copy(
                activePlayerIndex = nextIndex,
                eventMessage = "❄️ ${nextPlayer.name} is FROZEN! Turn skipped!"
            )
            scope.launch(Dispatchers.Default) {
                delay(1200L)
                passTurnToNextPlayer("Frozen player skipped")
            }
            return
        }

        _gameState.value = state.copy(
            activePlayerIndex = nextIndex,
            diceRoll = 0,
            hasRolled = false,
            isRolling = false,
            consecutiveSixes = 0,
            turnTimeRemaining = 15,
            movingTokenId = null,
            eventMessage = "${nextPlayer.name}'s turn!"
        )

        startTurnTimer()
        checkBotTurn()
    }

    private fun checkBotTurn() {
        val state = _gameState.value
        val activePlayer = state.activePlayer ?: return

        if (activePlayer.isBot && !state.isGameOver) {
            botTurnJob?.cancel()
            botTurnJob = scope.launch(Dispatchers.Default) {
                // Realistic bot pause before rolling
                delay(600L + Random.nextLong(200, 500))
                rollDice()

                // Wait for roll animation to complete
                delay(600L)
                val updatedState = _gameState.value
                val botRoll = updatedState.diceRoll

                if (updatedState.hasRolled && botRoll > 0) {
                    val bestTokenId = BotAI.chooseBestMove(
                        botPlayer = activePlayer,
                        allPlayers = updatedState.players,
                        diceRoll = botRoll
                    )

                    if (bestTokenId != -1) {
                        delay(500L + Random.nextLong(150, 400))
                        executeMove(bestTokenId)
                    }
                }
            }
        }
    }

    fun sendChatMessage(text: String, senderName: String, senderColor: LudoColor) {
        val msg = ChatMessage(
            senderName = senderName,
            senderColor = senderColor,
            text = text
        )
        val list = (_gameState.value.chatMessages + msg).takeLast(20)
        _gameState.value = _gameState.value.copy(chatMessages = list)
    }

    fun sendFloatingEmoji(emoji: String, senderName: String, senderColor: LudoColor) {
        val floating = FloatingEmoji(
            emoji = emoji,
            senderName = senderName,
            senderColor = senderColor,
            startXFraction = Random.nextFloat() * 0.6f + 0.2f,
            startYFraction = Random.nextFloat() * 0.4f + 0.4f
        )
        val list = (_gameState.value.floatingEmojis + floating).takeLast(8)
        _gameState.value = _gameState.value.copy(floatingEmojis = list)

        // Auto remove after 3.5 seconds
        scope.launch(Dispatchers.Default) {
            delay(3500L)
            _gameState.value = _gameState.value.copy(
                floatingEmojis = _gameState.value.floatingEmojis.filter { it.id != floating.id }
            )
        }
    }
}
