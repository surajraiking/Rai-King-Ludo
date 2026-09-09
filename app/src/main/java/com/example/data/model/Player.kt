package com.example.data.model

data class Player(
    val id: String,
    val name: String,
    val color: LudoColor,
    val avatarId: Int = 0,
    val isBot: Boolean = false,
    val isOnline: Boolean = true,
    val isReady: Boolean = true,
    val isHost: Boolean = false,
    val tokens: List<Token> = List(4) { Token(id = it, color = color) },
    val finishedRank: Int = 0
) {
    val hasWon: Boolean get() = tokens.all { it.isHome }
    val tokensHomeCount: Int get() = tokens.count { it.isHome }
    val tokensInYardCount: Int get() = tokens.count { it.isInYard }

    fun updateToken(tokenId: Int, newStepCount: Int): Player {
        val updatedTokens = tokens.map {
            if (it.id == tokenId) it.copy(stepCount = newStepCount) else it
        }
        return copy(tokens = updatedTokens)
    }

    fun hasValidMoves(diceRoll: Int): Boolean {
        if (hasWon) return false
        return tokens.any { it.canMove(diceRoll) }
    }
}
