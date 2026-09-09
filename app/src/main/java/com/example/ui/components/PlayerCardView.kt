package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AVATAR_OPTIONS
import com.example.data.model.Player

@Composable
fun PlayerCardView(
    player: Player,
    isActiveTurn: Boolean,
    turnSecondsRemaining: Int,
    modifier: Modifier = Modifier
) {
    val color = player.color
    val avatar = AVATAR_OPTIONS.getOrNull(player.avatarId) ?: AVATAR_OPTIONS[0]

    val infiniteTransition = rememberInfiniteTransition(label = "turn_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier
            .widthIn(min = 120.dp, max = 150.dp)
            .shadow(if (isActiveTurn) 8.dp else 2.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isActiveTurn) color.lightColor.copy(alpha = 0.9f) else Color.White
            )
            .border(
                width = if (isActiveTurn) 2.5.dp else 1.dp,
                color = if (isActiveTurn) color.primaryColor.copy(alpha = glowAlpha) else Color(0xFFCBD5E1),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Avatar with Countdown Timer Ring
            Box(
                modifier = Modifier.size(38.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isActiveTurn) {
                    val progress = (turnSecondsRemaining / 15f).coerceIn(0f, 1f)
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(38.dp),
                        color = if (turnSecondsRemaining <= 5) Color.Red else color.primaryColor,
                        strokeWidth = 3.dp,
                        trackColor = color.lightColor
                    )
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(avatar.backgroundHex)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = avatar.emoji, fontSize = 16.sp)
                }

                // Online/Bot indicator badge
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(if (player.isBot) Color(0xFF94A3B8) else Color(0xFF22C55E))
                        .border(1.dp, Color.White, CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }

            // Name and token status dots
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = player.name,
                        fontSize = 12.sp,
                        fontWeight = if (isActiveTurn) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (isActiveTurn) color.darkColor else Color(0xFF1E293B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (player.finishedRank > 0) {
                        Text(text = "👑 #${player.finishedRank}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                    }
                }

                // 4 Token status indicators: Home (Gold), OnBoard (Player color), Yard (Light gray)
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    player.tokens.forEach { token ->
                        val tokenDotColor = when {
                            token.isHome -> Color(0xFFF59E0B) // Gold
                            token.isOnBoard -> color.primaryColor
                            else -> Color(0xFFCBD5E1) // In yard
                        }
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(tokenDotColor)
                                .border(0.5.dp, Color.Black.copy(alpha = 0.3f), CircleShape)
                        )
                    }
                }
            }
        }
    }
}
