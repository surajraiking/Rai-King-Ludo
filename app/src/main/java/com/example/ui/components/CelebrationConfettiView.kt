package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AVATAR_OPTIONS
import com.example.data.model.Player
import kotlin.random.Random

private data class ConfettiParticle(
    val xRatio: Float,
    val initialY: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val rotationSpeed: Float
)

@Composable
fun CelebrationDialog(
    winner: Player,
    isCurrentUserWinner: Boolean,
    coinsWon: Long,
    onPlayAgain: () -> Unit,
    onBackToHome: () -> Unit
) {
    val particles = remember {
        val colors = listOf(
            Color(0xFFE53935), Color(0xFF43A047), Color(0xFFFDD835), Color(0xFF1E88E5),
            Color(0xFFEC4899), Color(0xFF8B5CF6), Color(0xFFF97316), Color(0xFF10B981)
        )
        List(60) {
            ConfettiParticle(
                xRatio = Random.nextFloat(),
                initialY = Random.nextFloat() * -500f,
                speed = Random.nextFloat() * 300f + 250f,
                size = Random.nextFloat() * 10f + 8f,
                color = colors.random(),
                rotationSpeed = Random.nextFloat() * 4f + 1f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_fall"
    )

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0F172A))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Animated falling confetti background canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasH = size.height
                val canvasW = size.width

                particles.forEach { p ->
                    val y = (p.initialY + animationProgress * (canvasH + 600f) * (p.speed / 300f)) % (canvasH + 100f)
                    val x = p.xRatio * canvasW + kotlin.math.sin((animationProgress * 10f + p.rotationSpeed).toDouble()).toFloat() * 20f

                    drawRect(
                        color = p.color,
                        topLeft = Offset(x, y),
                        size = Size(p.size, p.size * 1.5f)
                    )
                }
            }

            // Dialog Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Crown & Trophy Badge
                Text(
                    text = if (isCurrentUserWinner) "👑 VICTORY! 👑" else "GAME OVER",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isCurrentUserWinner) Color(0xFFFBBF24) else Color.White,
                    letterSpacing = 1.sp
                )

                // Winner Avatar
                val avatar = AVATAR_OPTIONS.getOrNull(winner.avatarId) ?: AVATAR_OPTIONS[0]
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color(avatar.backgroundHex))
                        .shadow(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = avatar.emoji, fontSize = 42.sp)
                }

                Text(
                    text = "${winner.name} Wins!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = if (isCurrentUserWinner) "Outstanding strategy! You dominated the board." else "Good try! Better luck in the next round.",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )

                // Coin reward card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "🪙", fontSize = 20.sp)
                        Text(
                            text = if (isCurrentUserWinner) "+$coinsWon Coins" else "+50 Consolation Coins",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFBBF24)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackToHome,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("celebration_home_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" Home", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onPlayAgain,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("celebration_play_again_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(" Rematch", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // Attribution footer
                Text(
                    text = "Rai Ludo King • Developed by Suraj Rai",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
