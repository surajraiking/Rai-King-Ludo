package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LudoColor
import com.example.data.model.LudoDiceState

@Composable
fun DiceView(
    diceState: LudoDiceState,
    onRollClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 68.dp
) {
    DiceView(
        diceValue = diceState.value,
        isRolling = diceState.isRolling,
        canRoll = diceState.canRoll,
        activeColor = diceState.activePlayerColor,
        onRollClick = onRollClick,
        modifier = modifier,
        size = size
    )
}

@Composable
fun DiceView(
    diceValue: Int,
    isRolling: Boolean,
    canRoll: Boolean,
    activeColor: LudoColor,
    onRollClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 68.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dice_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val rollingRotation by infiniteTransition.animateFloat(
        initialValue = -25f,
        targetValue = 25f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rolling_rot"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .scale(if (canRoll) pulseScale else 1f)
                .graphicsLayer {
                    if (isRolling) {
                        rotationZ = rollingRotation
                        rotationX = rollingRotation * 0.8f
                        cameraDistance = 12f * density
                    }
                }
                .shadow(
                    elevation = if (canRoll) 12.dp else 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = activeColor.primaryColor
                )
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = if (canRoll) 3.dp else 1.5.dp,
                    color = if (canRoll) activeColor.primaryColor else Color(0xFF94A3B8),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable(
                    enabled = canRoll,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onRollClick()
                }
                .testTag("dice_roll_button"),
            contentAlignment = Alignment.Center
        ) {
            DiceFaceCanvas(
                diceValue = if (diceValue in 1..6) diceValue else 1,
                isRolling = isRolling,
                primaryColor = activeColor.primaryColor
            )
        }

        if (canRoll) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(activeColor.primaryColor)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "ROLL",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun DiceFaceCanvas(
    diceValue: Int,
    isRolling: Boolean,
    primaryColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val corner = 16.dp.toPx()

        // 3D Beveled Dice Body with soft gradient
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White,
                    Color(0xFFF8FAFC),
                    Color(0xFFE2E8F0)
                )
            ),
            cornerRadius = CornerRadius(corner, corner)
        )

        // Subtle top bevel highlight
        drawRoundRect(
            color = Color.White.copy(alpha = 0.8f),
            topLeft = Offset(4f, 4f),
            size = Size(w - 8f, h * 0.35f),
            cornerRadius = CornerRadius(corner - 4f, corner - 4f)
        )

        // Draw Pips (Dots)
        val pipRadius = w * 0.088f
        val pipColor = if (diceValue == 1) Color(0xFFE11D48) else Color(0xFF1E293B)

        fun drawPip(xf: Float, yf: Float, col: Color = pipColor) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.3f), col, Color.Black.copy(alpha = 0.3f)),
                    center = Offset(w * xf - pipRadius * 0.2f, h * yf - pipRadius * 0.2f),
                    radius = pipRadius * 1.2f
                ),
                radius = pipRadius,
                center = Offset(w * xf, h * yf)
            )
        }

        when (diceValue) {
            1 -> {
                drawPip(0.5f, 0.5f, Color(0xFFE11D48))
            }
            2 -> {
                drawPip(0.28f, 0.28f)
                drawPip(0.72f, 0.72f)
            }
            3 -> {
                drawPip(0.28f, 0.28f)
                drawPip(0.5f, 0.5f)
                drawPip(0.72f, 0.72f)
            }
            4 -> {
                drawPip(0.28f, 0.28f)
                drawPip(0.72f, 0.28f)
                drawPip(0.28f, 0.72f)
                drawPip(0.72f, 0.72f)
            }
            5 -> {
                drawPip(0.26f, 0.26f)
                drawPip(0.74f, 0.26f)
                drawPip(0.5f, 0.5f)
                drawPip(0.26f, 0.74f)
                drawPip(0.74f, 0.74f)
            }
            6 -> {
                drawPip(0.28f, 0.24f)
                drawPip(0.28f, 0.50f)
                drawPip(0.28f, 0.76f)
                drawPip(0.72f, 0.24f)
                drawPip(0.72f, 0.50f)
                drawPip(0.72f, 0.76f)
            }
        }
    }
}
