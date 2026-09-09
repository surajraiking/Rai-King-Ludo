package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.BoardTheme
import com.example.data.model.ChatMessage
import com.example.data.model.GameState
import com.example.data.model.LudoColor
import com.example.data.model.Player
import com.example.data.model.PowerUpInventory
import com.example.data.model.PowerUpType
import com.example.ui.components.CelebrationDialog
import com.example.ui.components.DiceView
import com.example.ui.components.FloatingEmojiOverlay
import com.example.ui.components.InGameChatDialog
import com.example.ui.components.LudoBoardView
import com.example.ui.components.PlayerCardView
import com.example.ui.components.REACTION_EMOJIS

@Composable
fun GameScreen(
    gameState: GameState,
    currentUserId: String,
    soundEnabled: Boolean,
    powerUpInventory: PowerUpInventory = PowerUpInventory(),
    onUsePowerUp: (PowerUpType) -> Unit = {},
    onOpenLiveVoice: () -> Unit = {},
    onToggleSound: (Boolean) -> Unit,
    onRollDice: () -> Unit,
    onTokenClick: (Int) -> Unit,
    onSendChatMessage: (String) -> Unit,
    onSendEmojiReaction: (String) -> Unit,
    onPlayAgain: () -> Unit,
    onExitGame: () -> Unit
) {
    var showChatDialog by remember { mutableStateOf(false) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }

    val activePlayer = gameState.activePlayer ?: gameState.players.firstOrNull()
    val isMyTurn = (activePlayer != null && !activePlayer.isBot && gameState.isCurrentPlayerHuman)
    val canRoll = isMyTurn && !gameState.hasRolled && !gameState.isRolling

    val playerRed = gameState.players.find { it.color == LudoColor.RED }
    val playerGreen = gameState.players.find { it.color == LudoColor.GREEN }
    val playerYellow = gameState.players.find { it.color == LudoColor.YELLOW }
    val playerBlue = gameState.players.find { it.color == LudoColor.BLUE }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = when (gameState.boardTheme) {
                        BoardTheme.CLASSIC -> listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                        BoardTheme.ROYAL_DARK -> listOf(Color(0xFF0B1329), Color(0xFF111C3A))
                        BoardTheme.FESTIVE_EMERALD -> listOf(Color(0xFF022C22), Color(0xFF064E3B))
                    }
                )
            )
            .testTag("game_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top App Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { showExitConfirmDialog = true },
                    modifier = Modifier.testTag("game_exit_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit", tint = Color.White)
                }

                // Mode and Room Code Badge
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = gameState.gameMode.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFBBF24)
                    )
                    Text(
                        text = "Room: ${gameState.roomCode}",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onOpenLiveVoice,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B).copy(alpha = 0.25f))
                            .border(1.dp, Color(0xFFF59E0B), CircleShape)
                            .testTag("game_live_voice_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Live AI Voice (gemini-3.1-flash-live-preview)",
                            tint = Color(0xFFFBBF24)
                        )
                    }

                    IconButton(onClick = { onToggleSound(!soundEnabled) }) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                            contentDescription = "Sound",
                            tint = Color.White
                        )
                    }

                    IconButton(onClick = { showChatDialog = true }, modifier = Modifier.testTag("game_chat_button")) {
                        Icon(Icons.Default.Chat, contentDescription = "Chat", tint = Color(0xFF38BDF8))
                    }
                }
            }

            // Event Announcement Pill
            gameState.eventMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = msg,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFBBF24),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Top Player Cards: Red (Left) and Green (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                playerRed?.let {
                    PlayerCardView(
                        player = it,
                        isActiveTurn = (it.color == activePlayer?.color),
                        turnSecondsRemaining = gameState.turnTimeRemaining
                    )
                } ?: Spacer(modifier = Modifier.widthIn(min = 120.dp))

                playerGreen?.let {
                    PlayerCardView(
                        player = it,
                        isActiveTurn = (it.color == activePlayer?.color),
                        turnSecondsRemaining = gameState.turnTimeRemaining
                    )
                } ?: Spacer(modifier = Modifier.widthIn(min = 120.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Center Board & Interactive Playfield
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                LudoBoardView(
                    gameState = gameState,
                    boardTheme = gameState.boardTheme,
                    onTokenClick = onTokenClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                )

                // 3D Animated Dice in Center
                DiceView(
                    diceValue = gameState.diceRoll,
                    isRolling = gameState.isRolling,
                    canRoll = canRoll,
                    activeColor = activePlayer?.color ?: LudoColor.RED,
                    onRollClick = onRollDice,
                    size = 64.dp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom Player Cards: Blue (Left) and Yellow (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                playerBlue?.let {
                    PlayerCardView(
                        player = it,
                        isActiveTurn = (it.color == activePlayer?.color),
                        turnSecondsRemaining = gameState.turnTimeRemaining
                    )
                } ?: Spacer(modifier = Modifier.widthIn(min = 120.dp))

                playerYellow?.let {
                    PlayerCardView(
                        player = it,
                        isActiveTurn = (it.color == activePlayer?.color),
                        turnSecondsRemaining = gameState.turnTimeRemaining
                    )
                } ?: Spacer(modifier = Modifier.widthIn(min = 120.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Magic Power-Ups Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.95f))
                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PowerUpItemButton(
                    type = PowerUpType.DIVINE_SHIELD,
                    count = powerUpInventory.shields,
                    isMyTurn = isMyTurn,
                    onClick = { onUsePowerUp(PowerUpType.DIVINE_SHIELD) }
                )
                PowerUpItemButton(
                    type = PowerUpType.GOLDEN_DICE,
                    count = powerUpInventory.goldenDice,
                    isMyTurn = isMyTurn,
                    onClick = { onUsePowerUp(PowerUpType.GOLDEN_DICE) }
                )
                PowerUpItemButton(
                    type = PowerUpType.DOUBLE_BOOST,
                    count = powerUpInventory.speedBoosts,
                    isMyTurn = isMyTurn,
                    onClick = { onUsePowerUp(PowerUpType.DOUBLE_BOOST) }
                )
                PowerUpItemButton(
                    type = PowerUpType.FREEZE_SPELL,
                    count = powerUpInventory.freezeSpells,
                    isMyTurn = isMyTurn,
                    onClick = { onUsePowerUp(PowerUpType.FREEZE_SPELL) }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom Floating Emoji Reactions Bar
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E293B).copy(alpha = 0.9f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                REACTION_EMOJIS.forEach { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 22.sp,
                        modifier = Modifier
                            .clickable { onSendEmojiReaction(emoji) }
                            .padding(2.dp)
                    )
                }
            }
        }

        // Floating Emojis Drift Overlay
        FloatingEmojiOverlay(floatingEmojis = gameState.floatingEmojis)

        // Chat Dialog
        if (showChatDialog) {
            InGameChatDialog(
                chatMessages = gameState.chatMessages,
                onSendMessage = onSendChatMessage,
                onDismiss = { showChatDialog = false }
            )
        }

        // Exit Confirmation Dialog
        if (showExitConfirmDialog) {
            Dialog(onDismissRequest = { showExitConfirmDialog = false }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("Quit Match?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            "Are you sure you want to leave this game? Your progress will be forfeited.",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showExitConfirmDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Stay")
                            }
                            Button(
                                onClick = {
                                    showExitConfirmDialog = false
                                    onExitGame()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Leave")
                            }
                        }
                    }
                }
            }
        }

        // Celebration Confetti Dialog upon Winner
        if (gameState.isGameOver && gameState.winner != null) {
            val isUserWinner = (gameState.winner.id == currentUserId)
            CelebrationDialog(
                winner = gameState.winner,
                isCurrentUserWinner = isUserWinner,
                coinsWon = if (isUserWinner) 500L else 50L,
                onPlayAgain = onPlayAgain,
                onBackToHome = onExitGame
            )
        }
    }
}

@Composable
private fun PowerUpItemButton(
    type: PowerUpType,
    count: Int,
    isMyTurn: Boolean,
    onClick: () -> Unit
) {
    val isEnabled = isMyTurn && count > 0

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isEnabled) Color(0xFF1E293B) else Color(0xFF0F172A))
            .clickable(enabled = isEnabled) { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Text(text = type.icon, fontSize = 20.sp)
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .offset(x = 6.dp, y = (-4).dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "$count",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }
            }
        }
        Text(
            text = type.displayName.split(" ").firstOrNull() ?: type.displayName,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isEnabled) Color(0xFFFBBF24) else Color(0xFF64748B)
        )
    }
}

