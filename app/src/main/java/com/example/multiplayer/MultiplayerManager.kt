package com.example.multiplayer

import com.example.data.model.ChatMessage
import com.example.data.model.LudoColor
import com.example.data.model.Player
import com.example.data.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

data class RoomLobbyState(
    val roomCode: String = "",
    val isHost: Boolean = false,
    val isMatchmaking: Boolean = false,
    val isGameStarting: Boolean = false,
    val countdownSeconds: Int = 3,
    val players: List<Player> = emptyList(),
    val isQuickMatch: Boolean = false
)

class MultiplayerManager(private val scope: CoroutineScope) {

    private val _lobbyState = MutableStateFlow(RoomLobbyState())
    val lobbyState: StateFlow<RoomLobbyState> = _lobbyState.asStateFlow()

    private val botPoolNames = listOf(
        "Alex_Pro", "StarPlayer", "LudoMaster99", "ShadowKing",
        "CyberSamurai", "NeonKnight", "QueenElena", "LuckyDice7",
        "RaiWarrior", "AlphaStriker", "GrandLudo", "PixelHero"
    )

    fun createRoom(userProfile: UserProfile, maxPlayers: Int = 4): String {
        val code = (100000..999999).random().toString()
        val hostPlayer = Player(
            id = userProfile.id,
            name = userProfile.username,
            color = LudoColor.RED,
            avatarId = userProfile.avatarId,
            isBot = false,
            isOnline = true,
            isReady = true,
            isHost = true
        )

        _lobbyState.value = RoomLobbyState(
            roomCode = code,
            isHost = true,
            isMatchmaking = false,
            players = listOf(hostPlayer),
            isQuickMatch = false
        )

        return code
    }

    fun joinRoom(roomCode: String, userProfile: UserProfile, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (roomCode.length != 6) {
            onError("Please enter a valid 6-digit room code.")
            return
        }

        val guestPlayer = Player(
            id = userProfile.id,
            name = userProfile.username,
            color = LudoColor.GREEN,
            avatarId = userProfile.avatarId,
            isBot = false,
            isOnline = true,
            isReady = false,
            isHost = false
        )

        // Mock host player for joined room
        val hostPlayer = Player(
            id = "host_online",
            name = "RaiMaster",
            color = LudoColor.RED,
            avatarId = 0,
            isBot = true,
            isOnline = true,
            isReady = true,
            isHost = true
        )

        _lobbyState.value = RoomLobbyState(
            roomCode = roomCode,
            isHost = false,
            isMatchmaking = false,
            players = listOf(hostPlayer, guestPlayer),
            isQuickMatch = false
        )

        onSuccess()
    }

    fun startQuickMatch(userProfile: UserProfile, onGameReady: (List<Player>, String) -> Unit) {
        val code = (100000..999999).random().toString()
        val me = Player(
            id = userProfile.id,
            name = userProfile.username,
            color = LudoColor.RED,
            avatarId = userProfile.avatarId,
            isBot = false,
            isOnline = true,
            isReady = true,
            isHost = true
        )

        _lobbyState.value = RoomLobbyState(
            roomCode = code,
            isHost = true,
            isMatchmaking = true,
            players = listOf(me),
            isQuickMatch = true
        )

        // Simulate fast matchmaking pairing 3 real/bot players
        scope.launch(Dispatchers.Default) {
            val colors = listOf(LudoColor.GREEN, LudoColor.YELLOW, LudoColor.BLUE)
            val currentPlayers = mutableListOf(me)

            for (i in 0 until 3) {
                delay(600L + Random.nextLong(300, 700))
                val randomName = botPoolNames.random()
                val newPlayer = Player(
                    id = "online_${System.currentTimeMillis()}_$i",
                    name = randomName,
                    color = colors[i],
                    avatarId = Random.nextInt(0, 8),
                    isBot = true,
                    isOnline = true,
                    isReady = true
                )
                currentPlayers.add(newPlayer)
                _lobbyState.value = _lobbyState.value.copy(players = currentPlayers.toList())
            }

            // 3-second countdown to start
            _lobbyState.value = _lobbyState.value.copy(
                isMatchmaking = false,
                isGameStarting = true,
                countdownSeconds = 3
            )
            for (sec in 3 downTo 1) {
                _lobbyState.value = _lobbyState.value.copy(countdownSeconds = sec)
                delay(1000L)
            }

            onGameReady(currentPlayers, code)
        }
    }

    fun addBotToLobby() {
        val current = _lobbyState.value.players
        if (current.size >= 4) return

        val usedColors = current.map { it.color }.toSet()
        val nextColor = LudoColor.values().firstOrNull { it !in usedColors } ?: return
        val botName = botPoolNames.filter { name -> current.none { it.name == name } }.randomOrNull() ?: "Bot_${current.size + 1}"

        val botPlayer = Player(
            id = "bot_${System.currentTimeMillis()}",
            name = botName,
            color = nextColor,
            avatarId = Random.nextInt(0, 8),
            isBot = true,
            isOnline = true,
            isReady = true
        )

        _lobbyState.value = _lobbyState.value.copy(players = current + botPlayer)
    }

    fun toggleReady(playerId: String) {
        val updated = _lobbyState.value.players.map {
            if (it.id == playerId) it.copy(isReady = !it.isReady) else it
        }
        _lobbyState.value = _lobbyState.value.copy(players = updated)
    }

    fun resetLobby() {
        _lobbyState.value = RoomLobbyState()
    }
}
