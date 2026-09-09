package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.SoundManager
import com.example.ai.GeminiLiveVoiceDialog
import com.example.data.model.GameMode
import com.example.data.model.LudoColor
import com.example.data.model.Player
import com.example.data.model.PowerUpType
import com.example.data.storage.UserPreferences
import com.example.engine.LudoGameEngine
import com.example.multiplayer.MultiplayerManager
import com.example.ui.components.FortuneWheelDialog
import com.example.ui.screens.DailyRewardDialog
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.JoinRoomDialog
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.LobbyScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SearchGroundedArenaScreen
import com.example.ui.screens.SelectPlayerCountDialog
import com.example.ui.screens.SettingsDialog
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme

enum class AppScreen {
    SPLASH,
    HOME,
    LOBBY,
    GAME,
    LEADERBOARD,
    PROFILE,
    SEARCH_ARENA
}

class MainActivity : ComponentActivity() {

    private lateinit var soundManager: SoundManager
    private lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        soundManager = SoundManager(applicationContext)
        userPreferences = UserPreferences(applicationContext)

        setContent {
            MyApplicationTheme {
                LudoAppMain(soundManager, userPreferences)
            }
        }
    }
}

@Composable
fun LudoAppMain(
    soundManager: SoundManager,
    userPreferences: UserPreferences
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gameEngine = remember { LudoGameEngine(soundManager, scope) }
    val multiplayerManager = remember { MultiplayerManager(scope) }

    val userProfile by userPreferences.userProfile.collectAsState()
    val powerUpInventory by userPreferences.powerUpInventory.collectAsState()
    val soundEnabled by userPreferences.soundEnabled.collectAsState()
    val vibrationEnabled by userPreferences.vibrationEnabled.collectAsState()
    val boardTheme by userPreferences.boardTheme.collectAsState()

    val gameState by gameEngine.gameState.collectAsState()
    val lobbyState by multiplayerManager.lobbyState.collectAsState()

    var currentScreen by remember { mutableStateOf(AppScreen.SPLASH) }

    // Dialog states
    var showDailyRewardDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showJoinRoomDialog by remember { mutableStateOf(false) }
    var showFriendsChoiceDialog by remember { mutableStateOf(false) }
    var showVsBotCountDialog by remember { mutableStateOf(false) }
    var showLocalPlayCountDialog by remember { mutableStateOf(false) }
    var showLiveVoiceDialog by remember { mutableStateOf(false) }
    var showFortuneWheelDialog by remember { mutableStateOf(false) }

    // Sync audio and board settings
    LaunchedEffect(soundEnabled) {
        soundManager.isSoundEnabled = soundEnabled
    }
    LaunchedEffect(vibrationEnabled) {
        soundManager.isVibrationEnabled = vibrationEnabled
    }

    // Handle game completion stats record
    LaunchedEffect(gameState.isGameOver, gameState.winner) {
        if (gameState.isGameOver && gameState.winner != null) {
            val isWon = (gameState.winner?.id == userProfile.id)
            val coinReward = if (isWon) 500L else 50L
            userPreferences.recordMatchResult(isWon, coinReward)
        }
    }

    // Back handler
    BackHandler(enabled = currentScreen != AppScreen.HOME && currentScreen != AppScreen.SPLASH) {
        when (currentScreen) {
            AppScreen.LOBBY -> {
                multiplayerManager.resetLobby()
                currentScreen = AppScreen.HOME
            }
            AppScreen.GAME -> {
                currentScreen = AppScreen.HOME
            }
            AppScreen.LEADERBOARD, AppScreen.PROFILE -> {
                currentScreen = AppScreen.HOME
            }
            else -> {}
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.SPLASH -> {
                    SplashScreen(
                        onTimeout = {
                            currentScreen = AppScreen.HOME
                        }
                    )
                }

                AppScreen.HOME -> {
                    HomeScreen(
                        userProfile = userProfile,
                        onPlayOnline = {
                            soundManager.playButtonClick()
                            currentScreen = AppScreen.LOBBY
                            multiplayerManager.startQuickMatch(userProfile) { players, code ->
                                gameEngine.startNewGame(players, GameMode.ONLINE_MULTIPLAYER, code)
                                currentScreen = AppScreen.GAME
                            }
                        },
                        onPlayWithFriends = {
                            soundManager.playButtonClick()
                            showFriendsChoiceDialog = true
                        },
                        onPlayWithBot = {
                            soundManager.playButtonClick()
                            showVsBotCountDialog = true
                        },
                        onLocalPlay = {
                            soundManager.playButtonClick()
                            showLocalPlayCountDialog = true
                        },
                        onOpenSearchArena = {
                            soundManager.playButtonClick()
                            currentScreen = AppScreen.SEARCH_ARENA
                        },
                        onOpenLiveVoice = {
                            soundManager.playButtonClick()
                            showLiveVoiceDialog = true
                        },
                        onOpenFortuneWheel = {
                            soundManager.playButtonClick()
                            showFortuneWheelDialog = true
                        },
                        onOpenLeaderboard = {
                            soundManager.playButtonClick()
                            currentScreen = AppScreen.LEADERBOARD
                        },
                        onOpenProfile = {
                            soundManager.playButtonClick()
                            currentScreen = AppScreen.PROFILE
                        },
                        onOpenSettings = {
                            soundManager.playButtonClick()
                            showSettingsDialog = true
                        },
                        onOpenDailyReward = {
                            soundManager.playButtonClick()
                            showDailyRewardDialog = true
                        }
                    )
                }

                AppScreen.LOBBY -> {
                    LobbyScreen(
                        lobbyState = lobbyState,
                        currentUserId = userProfile.id,
                        onAddBot = {
                            soundManager.playButtonClick()
                            multiplayerManager.addBotToLobby()
                        },
                        onToggleReady = {
                            soundManager.playButtonClick()
                            multiplayerManager.toggleReady(userProfile.id)
                        },
                        onStartGame = {
                            soundManager.playButtonClick()
                            gameEngine.startNewGame(
                                players = lobbyState.players,
                                gameMode = GameMode.ONLINE_MULTIPLAYER,
                                roomCode = lobbyState.roomCode
                            )
                            currentScreen = AppScreen.GAME
                        },
                        onLeaveLobby = {
                            soundManager.playButtonClick()
                            multiplayerManager.resetLobby()
                            currentScreen = AppScreen.HOME
                        }
                    )
                }

                AppScreen.GAME -> {
                    GameScreen(
                        gameState = gameState.copy(boardTheme = boardTheme),
                        currentUserId = userProfile.id,
                        soundEnabled = soundEnabled,
                        powerUpInventory = powerUpInventory,
                        onUsePowerUp = { type ->
                            if (userPreferences.consumePowerUp(type)) {
                                soundManager.playButtonClick()
                                gameEngine.applyPowerUp(type, gameState.activePlayer?.color ?: LudoColor.RED)
                            } else {
                                Toast.makeText(context, "No ${type.displayName} left! Win more in Fortune Wheel!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onOpenLiveVoice = {
                            soundManager.playButtonClick()
                            showLiveVoiceDialog = true
                        },
                        onToggleSound = { userPreferences.setSoundEnabled(it) },
                        onRollDice = {
                            gameEngine.rollDice()
                        },
                        onTokenClick = { tokenId ->
                            gameEngine.executeMove(tokenId)
                        },
                        onSendChatMessage = { text ->
                            gameEngine.sendChatMessage(
                                text = text,
                                senderName = userProfile.username,
                                senderColor = gameState.activePlayer?.color ?: LudoColor.RED
                            )
                        },
                        onSendEmojiReaction = { emoji ->
                            gameEngine.sendFloatingEmoji(
                                emoji = emoji,
                                senderName = userProfile.username,
                                senderColor = gameState.activePlayer?.color ?: LudoColor.RED
                            )
                        },
                        onPlayAgain = {
                            soundManager.playButtonClick()
                            // Rematch with same players
                            gameEngine.startNewGame(
                                players = gameState.players.map { it.copy(tokens = List(4) { idx -> com.example.data.model.Token(idx, it.color) }, finishedRank = 0) },
                                gameMode = gameState.gameMode,
                                roomCode = gameState.roomCode
                            )
                        },
                        onExitGame = {
                            soundManager.playButtonClick()
                            currentScreen = AppScreen.HOME
                        }
                    )
                }

                AppScreen.LEADERBOARD -> {
                    LeaderboardScreen(
                        userProfile = userProfile,
                        onBack = {
                            soundManager.playButtonClick()
                            currentScreen = AppScreen.HOME
                        }
                    )
                }

                AppScreen.PROFILE -> {
                    ProfileScreen(
                        userProfile = userProfile,
                        onUpdateProfile = { newName, newAvatarId ->
                            soundManager.playButtonClick()
                            userPreferences.updateProfile(newName, newAvatarId)
                        },
                        onBack = {
                            soundManager.playButtonClick()
                            currentScreen = AppScreen.HOME
                        }
                    )
                }

                AppScreen.SEARCH_ARENA -> {
                    SearchGroundedArenaScreen(
                        userProfile = userProfile,
                        onAwardCoins = { coins: Long ->
                            userPreferences.addCoins(coins)
                        },
                        onBack = {
                            soundManager.playButtonClick()
                            currentScreen = AppScreen.HOME
                        }
                    )
                }
            }

            // Dialogs
            if (showFriendsChoiceDialog) {
                Dialog(onDismissRequest = { showFriendsChoiceDialog = false }) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "👥 Play with Friends",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "Create a new private room or join an existing room with code.",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8)
                            )

                            Button(
                                onClick = {
                                    showFriendsChoiceDialog = false
                                    multiplayerManager.createRoom(userProfile)
                                    currentScreen = AppScreen.LOBBY
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Text("Create Room", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    showFriendsChoiceDialog = false
                                    showJoinRoomDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Join Room with Code", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (showJoinRoomDialog) {
                JoinRoomDialog(
                    onJoin = { code ->
                        multiplayerManager.joinRoom(
                            roomCode = code,
                            userProfile = userProfile,
                            onSuccess = {
                                showJoinRoomDialog = false
                                currentScreen = AppScreen.LOBBY
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    onDismiss = { showJoinRoomDialog = false }
                )
            }

            if (showVsBotCountDialog) {
                SelectPlayerCountDialog(
                    title = "🤖 Play Vs Computer",
                    onConfirm = { count ->
                        showVsBotCountDialog = false
                        val colors = listOf(LudoColor.RED, LudoColor.GREEN, LudoColor.YELLOW, LudoColor.BLUE)
                        val botNames = listOf("Alex Bot", "Samurai AI", "Dragon AI")
                        val players = mutableListOf(
                            Player(
                                id = userProfile.id,
                                name = userProfile.username,
                                color = LudoColor.RED,
                                avatarId = userProfile.avatarId,
                                isBot = false
                            )
                        )
                        for (i in 1 until count) {
                            players.add(
                                Player(
                                    id = "bot_$i",
                                    name = botNames.getOrElse(i - 1) { "Bot $i" },
                                    color = colors[i],
                                    avatarId = (i + 2) % 8,
                                    isBot = true
                                )
                            )
                        }
                        gameEngine.startNewGame(players, GameMode.PLAY_VS_BOT)
                        currentScreen = AppScreen.GAME
                    },
                    onDismiss = { showVsBotCountDialog = false }
                )
            }

            if (showLocalPlayCountDialog) {
                SelectPlayerCountDialog(
                    title = "🎲 Local Pass & Play",
                    onConfirm = { count ->
                        showLocalPlayCountDialog = false
                        val colors = listOf(LudoColor.RED, LudoColor.GREEN, LudoColor.YELLOW, LudoColor.BLUE)
                        val players = (0 until count).map { i ->
                            Player(
                                id = "local_p$i",
                                name = if (i == 0) userProfile.username else "Player ${i + 1}",
                                color = colors[i],
                                avatarId = i % 8,
                                isBot = false
                            )
                        }
                        gameEngine.startNewGame(players, GameMode.LOCAL_PASS_AND_PLAY)
                        currentScreen = AppScreen.GAME
                    },
                    onDismiss = { showLocalPlayCountDialog = false }
                )
            }

            if (showDailyRewardDialog) {
                DailyRewardDialog(
                    onClaim = {
                        val claimed = userPreferences.claimDailyReward(500L)
                        showDailyRewardDialog = false
                        if (claimed) {
                            soundManager.playVictory()
                            Toast.makeText(context, "🎉 500 Coins added to your account!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "You have already claimed today's bonus! Come back tomorrow.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDismiss = { showDailyRewardDialog = false }
                )
            }

            if (showSettingsDialog) {
                SettingsDialog(
                    soundEnabled = soundEnabled,
                    vibrationEnabled = vibrationEnabled,
                    currentTheme = boardTheme,
                    onSoundToggle = { userPreferences.setSoundEnabled(it) },
                    onVibrationToggle = { userPreferences.setVibrationEnabled(it) },
                    onThemeSelect = { userPreferences.setBoardTheme(it) },
                    onDismiss = { showSettingsDialog = false }
                )
            }

            if (showFortuneWheelDialog) {
                FortuneWheelDialog(
                    coins = userProfile.coins,
                    onSpinCost = {
                        userPreferences.addCoins(-100L)
                    },
                    onRewardWon = { rewardCoins: Long, rewardPowerUp: PowerUpType? ->
                        soundManager.playVictory()
                        if (rewardCoins > 0L) {
                            userPreferences.addCoins(rewardCoins)
                        }
                        if (rewardPowerUp != null) {
                            userPreferences.addPowerUp(rewardPowerUp)
                        }
                    },
                    onDismiss = { showFortuneWheelDialog = false }
                )
            }

            if (showLiveVoiceDialog) {
                GeminiLiveVoiceDialog(
                    onDismiss = { showLiveVoiceDialog = false }
                )
            }
        }
    }
}
