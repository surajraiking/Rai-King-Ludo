package com.example.data.model

data class GameState(
    val gameId: String = java.util.UUID.randomUUID().toString(),
    val gameMode: GameMode = GameMode.PLAY_VS_BOT,
    val roomCode: String = "749281",
    val players: List<Player> = emptyList(),
    val activePlayerIndex: Int = 0,
    val diceRoll: Int = 0,
    val isRolling: Boolean = false,
    val hasRolled: Boolean = false,
    val consecutiveSixes: Int = 0,
    val turnTimeRemaining: Int = 15,
    val movingTokenId: Int? = null,
    val isGameOver: Boolean = false,
    val winner: Player? = null,
    val rankings: List<Player> = emptyList(),
    val eventMessage: String? = null,
    val chatMessages: List<ChatMessage> = emptyList(),
    val floatingEmojis: List<FloatingEmoji> = emptyList(),
    val boardTheme: BoardTheme = BoardTheme.CLASSIC
) {
    val activePlayer: Player?
        get() = players.getOrNull(activePlayerIndex)

    val validMovableTokenIds: List<Int>
        get() {
            if (!hasRolled || isRolling || diceRoll == 0) return emptyList()
            val player = activePlayer ?: return emptyList()
            return player.tokens.filter { it.canMove(diceRoll) }.map { it.id }
        }

    val isCurrentPlayerHuman: Boolean
        get() {
            val player = activePlayer ?: return false
            return !player.isBot
        }
}
