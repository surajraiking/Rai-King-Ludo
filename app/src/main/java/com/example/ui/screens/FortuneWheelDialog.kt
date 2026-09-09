package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.FortuneWheelSlot
import com.example.data.model.PowerUpType
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun FortuneWheelDialog(
    onRewardWon: (coins: Int, powerUp: PowerUpType?) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isSpinning by remember { mutableStateOf(false) }
    var wonSlot by remember { mutableStateOf<FortuneWheelSlot?>(null) }
    val rotationAnim = remember { Animatable(0f) }

    val slots = remember {
        listOf(
            FortuneWheelSlot("🪙 250 Coins", rewardCoins = 250, colorHex = 0xFFEF4444),
            FortuneWheelSlot("🛡️ Shield", powerUp = PowerUpType.DIVINE_SHIELD, colorHex = 0xFF3B82F6),
            FortuneWheelSlot("🪙 500 Coins", rewardCoins = 500, colorHex = 0xFF10B981),
            FortuneWheelSlot("🎲 Sixer", powerUp = PowerUpType.GOLDEN_DICE, colorHex = 0xFFF59E0B),
            FortuneWheelSlot("⚡ Boost", powerUp = PowerUpType.DOUBLE_BOOST, colorHex = 0xFF8B5CF6),
            FortuneWheelSlot("🪙 1000 Coins", rewardCoins = 1000, colorHex = 0xFFEC4899),
            FortuneWheelSlot("❄️ Freeze", powerUp = PowerUpType.FREEZE_SPELL, colorHex = 0xFF06B6D4),
            FortuneWheelSlot("👑 JACKPOT 2.5K", rewardCoins = 2500, colorHex = 0xFFEAB308)
        )
    }

    val sliceAngle = 360f / slots.size

    Dialog(onDismissRequest = { if (!isSpinning) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("fortune_wheel_dialog"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFF59E0B))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎡 Lucky Spin & Win",
                        color = Color(0xFFFCD34D),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isSpinning
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Text(
                    text = "Spin daily for Magic Power-ups and Coin Jackpots!",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // The Wheel Stage
                Box(
                    modifier = Modifier
                        .size(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Wheel Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(rotationAnim.value)
                    ) {
                        val radius = size.minDimension / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)

                        slots.forEachIndexed { index, slot ->
                            val startAngle = index * sliceAngle
                            drawArc(
                                color = Color(slot.colorHex),
                                startAngle = startAngle,
                                sweepAngle = sliceAngle,
                                useCenter = true,
                                size = Size(radius * 2, radius * 2),
                                topLeft = Offset(center.x - radius, center.y - radius)
                            )

                            // Sector Border
                            drawArc(
                                color = Color.White.copy(alpha = 0.3f),
                                startAngle = startAngle,
                                sweepAngle = sliceAngle,
                                useCenter = true,
                                style = Stroke(width = 2f),
                                size = Size(radius * 2, radius * 2),
                                topLeft = Offset(center.x - radius, center.y - radius)
                            )
                        }

                        // Outer gold rim
                        drawCircle(
                            color = Color(0xFFF59E0B),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 8f)
                        )
                    }

                    // Top Pointer Triangle
                    Canvas(
                        modifier = Modifier
                            .size(30.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        val path = Path().apply {
                            moveTo(size.width / 2f, size.height)
                            lineTo(0f, 0f)
                            lineTo(size.width, 0f)
                            close()
                        }
                        drawPath(path, color = Color(0xFFFDE68A))
                        drawPath(path, color = Color.White, style = Stroke(width = 2f))
                    }

                    // Center Hub Button
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A))
                            .border(3.dp, Color(0xFFF59E0B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "👑", fontSize = 22.sp)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                wonSlot?.let { slot ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🎉 You Won: ${slot.label}!",
                                color = Color(0xFF34D399),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            slot.powerUp?.let { p ->
                                Text(
                                    text = p.description,
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Button(
                    onClick = {
                        if (!isSpinning) {
                            scope.launch {
                                isSpinning = true
                                wonSlot = null
                                val winningIndex = Random.nextInt(slots.size)
                                // Wheel rotates clockwise, top pointer is at 270 degrees
                                val targetAngle = (360f * 6) + (360f - (winningIndex * sliceAngle + sliceAngle / 2f) + 270f) % 360f

                                rotationAnim.animateTo(
                                    targetValue = rotationAnim.value + targetAngle,
                                    animationSpec = tween(
                                        durationMillis = 3500,
                                        easing = FastOutSlowInEasing
                                    )
                                )

                                val reward = slots[winningIndex]
                                wonSlot = reward
                                onRewardWon(reward.rewardCoins, reward.powerUp)
                                isSpinning = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("spin_wheel_button"),
                    enabled = !isSpinning,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (isSpinning) "Spinning the Wheel..." else "SPIN THE WHEEL 🎯",
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
