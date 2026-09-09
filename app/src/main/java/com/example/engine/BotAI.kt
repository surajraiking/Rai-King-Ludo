package com.example.engine

import com.example.data.model.Player
import com.example.data.model.Token
import kotlin.random.Random

object BotAI {

    /**
     * Evaluates all movable tokens for the bot and chooses the best tactical move.
     * Returns tokenId to move, or -1 if no valid moves.
     */
    fun chooseBestMove(
        botPlayer: Player,
        allPlayers: List<Player>,
        diceRoll: Int
    ): Int {
        val movableTokens = botPlayer.tokens.filter { it.canMove(diceRoll) }
        if (movableTokens.isEmpty()) return -1
        if (movableTokens.size == 1) return movableTokens.first().id

        var bestTokenId = movableTokens.first().id
        var bestScore = Int.MIN_VALUE

        for (token in movableTokens) {
            val score = evaluateMoveScore(token, botPlayer, allPlayers, diceRoll)
            if (score > bestScore) {
                bestScore = score
                bestTokenId = token.id
            }
        }

        return bestTokenId
    }

    private fun evaluateMoveScore(
        token: Token,
        botPlayer: Player,
        allPlayers: List<Player>,
        diceRoll: Int
    ): Int {
        var score = 0

        // 1. Moving out of Yard to start on a 6
        if (token.isInYard && diceRoll == 6) {
            score += 85
            // Extra bonus if start cell has opponent to capture!
            val startGlobalIdx = botPlayer.color.startTrackIndex
            val canCaptureOnStart = allPlayers.filter { it.color != botPlayer.color }
                .any { opponent ->
                    opponent.tokens.any { oppToken ->
                        oppToken.globalTrackIndex == startGlobalIdx && !LudoBoardCoordinates.isSafeCell(startGlobalIdx)
                    }
                }
            if (canCaptureOnStart) score += 40
            return score
        }

        val destStepCount = token.stepCount + diceRoll

        // 2. Reaching Home exactly
        if (destStepCount == 56) {
            score += 120
            return score
        }

        // 3. Entering Home stretch (steps 51..55, safe from captures)
        if (destStepCount in 51..55) {
            score += 70 + (destStepCount - 50) * 5
        }

        // 4. Capture opponent token on common track
        if (destStepCount in 0..50) {
            val destGlobalIdx = (botPlayer.color.startTrackIndex + destStepCount) % 52
            val isSafe = LudoBoardCoordinates.isSafeCell(destGlobalIdx)

            if (!isSafe) {
                val opponentCaptured = allPlayers.filter { it.color != botPlayer.color }
                    .any { opponent ->
                        opponent.tokens.any { oppToken -> oppToken.globalTrackIndex == destGlobalIdx }
                    }
                if (opponentCaptured) {
                    score += 150 // Top priority: capture!
                }
            } else {
                score += 55 // Landing on safe star
            }

            // 5. Escaping danger: check if token was currently exposed and now reaches safety
            val currentGlobalIdx = token.globalTrackIndex
            if (currentGlobalIdx >= 0 && !LudoBoardCoordinates.isSafeCell(currentGlobalIdx)) {
                val inDanger = allPlayers.filter { it.color != botPlayer.color }
                    .any { opponent ->
                        opponent.tokens.any { oppToken ->
                            val dist = (currentGlobalIdx - oppToken.globalTrackIndex + 52) % 52
                            dist in 1..6
                        }
                    }
                if (inDanger) {
                    score += 45
                }
            }
        }

        // 6. Prefer advancing tokens closer to home
        score += destStepCount

        // 7. Add small jitter
        score += Random.nextInt(0, 5)

        return score
    }
}
