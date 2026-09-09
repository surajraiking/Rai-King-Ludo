package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.PowerUpType
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class WheelSlice(
    val label: String,
    val color: Color,
    val coinReward: Long = 0L,
    val powerUp: PowerUpType? = null
)

@Composable
fun FortuneWheelDialog(
    coins: Long,
    onSpinCost: () -> Unit,
    onRewardWon: (Long, PowerUpType?) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val rotation = remember { Animatable(0f) }
    var isSpinning by remember { mutableStateOf(false) }
    var wonReward by remember { mutableStateOf<WheelSlice?>(null) }

    val slices = remember {
        listOf(
            WheelSlice("🪙 500", Color(0xFFEF4444), coinReward = 500L),
            WheelSlice("🛡️ Shield", Color(0xFF3B82F6), powerUp = PowerUpType.DIVINE_SHIELD),
            WheelSlice("🪙 2,000", Color(0xFFF59E0B), coinReward = 2000L),
            WheelSlice("🎲 Golden", Color(0xFF8B5CF6), powerUp = PowerUpType.GOLDEN_DICE),
            WheelSlice("🪙 300", Color(0xFF10B981), coinReward = 300L),
            WheelSlice("⚡ Boost", Color(0xFFEC4899), powerUp = PowerUpType.DOUBLE_BOOST),
            WheelSlice("🪙 1,000", Color(0xFF06B6D4), coinReward = 1000L),
            WheelSlice("❄️ Freeze", Color(0xFF0D9488), powerUp = PowerUpType.FREEZE_SPELL)
        )
    }

    val sliceCount = slices.size
    val sliceAngle = 360f / sliceCount

    fun spinWheel() {
        if (isSpinning || coins < 100) return
        onSpinCost()
        isSpinning = true
        wonReward = null

        scope.launch {
            // Pick a winning slice randomly
            val winningIndex = Random.nextInt(sliceCount)
            val fullRotations = 5 + Random.nextInt(3) // 5 to 7 full circles
            // The needle is at top (270 degrees). Target angle calculation
            val targetSliceCenter = (winningIndex * sliceAngle) + (sliceAngle / 2f)
            val finalAngle = (fullRotations * 360f) + (360f - targetSliceCenter + 270f) % 360f + (fullRotations * 360f)

            rotation.animateTo(
                targetValue = rotation.value + finalAngle,
                animationSpec = tween(
                    durationMillis = 3800,
                    easing = CubicBezierEasing(0.15f, 0.85f, 0.25f, 1f)
                )
            )

            val won = slices[winningIndex]
            wonReward = won
            isSpinning = false
            onRewardWon(won.coinReward, won.powerUp)
        }
    }

    Dialog(onDismissRequest = { if (!isSpinning) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(
                2.dp,
                Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFF8B5CF6), Color(0xFF3B82F6)))
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .testTag("fortune_wheel_dialog")
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🎡 Lucky Fortune Wheel",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFBBF24)
                        )
                        Text(
                            text = "Spin & Win Coins & Tactical Power-ups!",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isSpinning,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                    }
                }

                // Balance Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Balance: 🪙 %,d Coins".format(coins),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFBBF24)
                    )
                }

                // Wheel Container with Needle
                Box(
                    modifier = Modifier
                        .size(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Wheel Canvas
                    Canvas(
                        modifier = Modifier
                            .size(220.dp)
                            .rotate(rotation.value)
                            .shadow(12.dp, CircleShape)
                    ) {
                        val canvasRadius = size.minDimension / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)

                        // Draw outer glow border
                        drawCircle(
                            color = Color(0xFFF59E0B),
                            radius = canvasRadius,
                            style = Stroke(width = 8f)
                        )

                        // Draw Slices
                        for (i in 0 until sliceCount) {
                            val startAngle = i * sliceAngle
                            drawArc(
                                color = slices[i].color,
                                startAngle = startAngle,
                                sweepAngle = sliceAngle,
                                useCenter = true,
                                topLeft = Offset(center.x - canvasRadius, center.y - canvasRadius),
                                size = Size(canvasRadius * 2, canvasRadius * 2),
                                style = Fill
                            )

                            // Slice divider
                            val angleRad = (startAngle * PI / 180f).toFloat()
                            val lineEnd = Offset(
                                x = center.x + canvasRadius * cos(angleRad),
                                y = center.y + canvasRadius * sin(angleRad)
                            )
                            drawLine(
                                color = Color.White.copy(alpha = 0.6f),
                                start = center,
                                end = lineEnd,
                                strokeWidth = 3f
                            )
                        }

                        // Inner Center Hub
                        drawCircle(
                            color = Color(0xFF0F172A),
                            radius = canvasRadius * 0.22f
                        )
                        drawCircle(
                            color = Color(0xFFFBBF24),
                            radius = canvasRadius * 0.22f,
                            style = Stroke(width = 4f)
                        )
                    }

                    // Slices Labels Overlay
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .rotate(rotation.value),
                        contentAlignment = Alignment.Center
                    ) {
                        for (i in 0 until sliceCount) {
                            val midAngle = (i * sliceAngle) + (sliceAngle / 2f)
                            Box(
                                modifier = Modifier
                                    .rotate(midAngle)
                                    .offset(y = (-68).dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = slices[i].label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Top Needle Indicator
                    Canvas(
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.TopCenter)
                            .offset(y = (-4).dp)
                    ) {
                        val path = Path().apply {
                            moveTo(size.width / 2f, size.height)
                            lineTo(0f, 0f)
                            lineTo(size.width, 0f)
                            close()
                        }
                        drawPath(path, color = Color(0xFFFBBF24))
                        drawPath(path, color = Color.White, style = Stroke(width = 2f))
                    }

                    // Center Gold Star
                    Text(
                        text = "⭐",
                        fontSize = 18.sp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Reward Announcement Card
                wonReward?.let { won ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🎉 CONGRATULATIONS! 🎉",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFBBF24)
                            )
                            Text(
                                text = "You won: ${won.label}!",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Spin Button
                Button(
                    onClick = { spinWheel() },
                    enabled = !isSpinning && coins >= 100,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("spin_wheel_button")
                ) {
                    Text(
                        text = if (isSpinning) "Spinning Wheel..." else "SPIN NOW (Cost: 100 🪙)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}
