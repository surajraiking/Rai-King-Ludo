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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BoardTheme
import com.example.data.model.GameState
import com.example.data.model.LudoColor
import com.example.data.model.Player
import com.example.data.model.Token
import com.example.engine.GridCoord
import com.example.engine.LudoBoardCoordinates
import kotlin.math.min

@Composable
fun LudoBoardView(
    gameState: GameState,
    boardTheme: BoardTheme,
    onTokenClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "token_scale"
    )

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(12.dp)
            .border(
                width = 3.dp,
                color = when (boardTheme) {
                    BoardTheme.CLASSIC -> Color(0xFFD4AF37) // Metallic Gold
                    BoardTheme.ROYAL_DARK -> Color(0xFF38BDF8) // Neon Cyan
                    BoardTheme.FESTIVE_EMERALD -> Color(0xFFFBBF24) // Amber
                }
            )
            .testTag("ludo_board_canvas")
    ) {
        val boardSizePx = min(constraints.maxWidth, constraints.maxHeight).toFloat()
        val cellSizePx = boardSizePx / 15f
        val density = androidx.compose.ui.platform.LocalDensity.current
        val cellSizeDp = with(density) { cellSizePx.toDp() }

        // 1. Draw static grid, yards, corridors, safe stars, and home triangles
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLudoBoard(boardSizePx, cellSizePx, boardTheme)
        }

        // 2. Render all player tokens with interactive touch targets
        val activePlayer = gameState.activePlayer
        val validMovableIds = gameState.validMovableTokenIds
        val canMoveActiveTokens = gameState.isCurrentPlayerHuman && validMovableIds.isNotEmpty()

        gameState.players.forEach { player ->
            val isCurrentPlayerTurn = (player.color == activePlayer?.color)

            // Group tokens to handle overlapping on the same cell
            val groupedTokens = player.tokens.groupBy { token ->
                LudoBoardCoordinates.getTokenGridCoord(token.color, token.id, token.stepCount)
            }

            groupedTokens.forEach { (coord, tokensAtCoord) ->
                tokensAtCoord.forEachIndexed { index, token ->
                    val isMovable = isCurrentPlayerTurn && canMoveActiveTokens && (token.id in validMovableIds)
                    val offsetSubX = if (tokensAtCoord.size > 1) (index % 2 - 0.5f) * 0.32f else 0f
                    val offsetSubY = if (tokensAtCoord.size > 1) (index / 2 - 0.5f) * 0.32f else 0f

                    val tokenX = cellSizeDp * (coord.col + offsetSubX)
                    val tokenY = cellSizeDp * (coord.row + offsetSubY)

                    Box(
                        modifier = Modifier
                            .offset(x = tokenX, y = tokenY)
                            .size(cellSizeDp)
                            .scale(if (isMovable) pulseScale else 1.0f)
                            .clickable(
                                enabled = isMovable,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onTokenClick(token.id)
                            }
                            .testTag("token_${token.color.name}_${token.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        TokenPiece(
                            token = token,
                            isMovable = isMovable,
                            size = cellSizeDp * 0.82f
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TokenPiece(
    token: Token,
    isMovable: Boolean,
    size: Dp
) {
    val color = token.color
    Box(
        modifier = Modifier
            .size(size)
            .shadow(if (isMovable) 10.dp else 4.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f),
                        color.primaryColor,
                        color.darkColor
                    )
                )
            )
            .border(
                width = if (isMovable) 2.5.dp else 1.5.dp,
                color = if (isMovable) Color.White else Color.Black.copy(alpha = 0.4f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        // Inner shiny core ring
        Box(
            modifier = Modifier
                .size(size * 0.46f)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.9f))
                .border(1.dp, color.darkColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (token.isHome) "👑" else "${token.id + 1}",
                color = color.darkColor,
                fontSize = if (token.isHome) 9.sp else 10.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

private fun DrawScope.drawLudoBoard(boardSize: Float, cellSize: Float, theme: BoardTheme) {
    val bgCol = when (theme) {
        BoardTheme.CLASSIC -> Color(0xFFFFFBEA)
        BoardTheme.ROYAL_DARK -> Color(0xFF0F172A)
        BoardTheme.FESTIVE_EMERALD -> Color(0xFF022C22)
    }
    val gridLineCol = when (theme) {
        BoardTheme.CLASSIC -> Color(0xFFCBD5E1)
        BoardTheme.ROYAL_DARK -> Color(0xFF334155)
        BoardTheme.FESTIVE_EMERALD -> Color(0xFF065F46)
    }

    // Base background
    drawRect(color = bgCol, size = Size(boardSize, boardSize))

    // 1. Draw 4 Corner Yards
    drawYard(0f, 0f, cellSize * 6, cellSize * 6, LudoColor.RED, theme)
    drawYard(cellSize * 9, 0f, cellSize * 6, cellSize * 6, LudoColor.GREEN, theme)
    drawYard(cellSize * 9, cellSize * 9, cellSize * 6, cellSize * 6, LudoColor.YELLOW, theme)
    drawYard(0f, cellSize * 9, cellSize * 6, cellSize * 6, LudoColor.BLUE, theme)

    // 2. Draw Track Cells
    for (col in 0..14) {
        for (row in 0..14) {
            val inYard = (col < 6 && row < 6) ||
                    (col > 8 && row < 6) ||
                    (col < 6 && row > 8) ||
                    (col > 8 && row > 8)
            val inCenter = (col in 6..8 && row in 6..8)

            if (!inYard && !inCenter) {
                val left = col * cellSize
                val top = row * cellSize

                // Default track cell outline
                drawRect(
                    color = gridLineCol,
                    topLeft = Offset(left, top),
                    size = Size(cellSize, cellSize),
                    style = Stroke(width = 1f)
                )

                // Home corridors highlight
                if (row == 7 && col in 1..5) {
                    drawRect(color = LudoColor.RED.primaryColor, topLeft = Offset(left, top), size = Size(cellSize, cellSize))
                } else if (col == 7 && row in 1..5) {
                    drawRect(color = LudoColor.GREEN.primaryColor, topLeft = Offset(left, top), size = Size(cellSize, cellSize))
                } else if (row == 7 && col in 9..13) {
                    drawRect(color = LudoColor.YELLOW.primaryColor, topLeft = Offset(left, top), size = Size(cellSize, cellSize))
                } else if (col == 7 && row in 9..13) {
                    drawRect(color = LudoColor.BLUE.primaryColor, topLeft = Offset(left, top), size = Size(cellSize, cellSize))
                }

                // Start cells highlight
                if (col == 1 && row == 6) {
                    drawRect(color = LudoColor.RED.primaryColor, topLeft = Offset(left, top), size = Size(cellSize, cellSize))
                    drawStar(left + cellSize / 2, top + cellSize / 2, cellSize * 0.36f, Color.White)
                } else if (col == 8 && row == 1) {
                    drawRect(color = LudoColor.GREEN.primaryColor, topLeft = Offset(left, top), size = Size(cellSize, cellSize))
                    drawStar(left + cellSize / 2, top + cellSize / 2, cellSize * 0.36f, Color.White)
                } else if (col == 13 && row == 8) {
                    drawRect(color = LudoColor.YELLOW.primaryColor, topLeft = Offset(left, top), size = Size(cellSize, cellSize))
                    drawStar(left + cellSize / 2, top + cellSize / 2, cellSize * 0.36f, Color.White)
                } else if (col == 6 && row == 13) {
                    drawRect(color = LudoColor.BLUE.primaryColor, topLeft = Offset(left, top), size = Size(cellSize, cellSize))
                    drawStar(left + cellSize / 2, top + cellSize / 2, cellSize * 0.36f, Color.White)
                }

                // Corner Star safe cells
                if ((col == 6 && row == 2) || (col == 12 && row == 6) || (col == 8 && row == 12) || (col == 2 && row == 8)) {
                    drawStar(left + cellSize / 2, top + cellSize / 2, cellSize * 0.38f, Color(0xFFF59E0B))
                }
            }
        }
    }

    // 3. Draw Center Home Triangles
    val centerLeft = cellSize * 6
    val centerTop = cellSize * 6
    val centerRight = cellSize * 9
    val centerBottom = cellSize * 9
    val centerX = cellSize * 7.5f
    val centerY = cellSize * 7.5f

    // Red (Left)
    drawPath(
        path = Path().apply {
            moveTo(centerLeft, centerTop)
            lineTo(centerX, centerY)
            lineTo(centerLeft, centerBottom)
            close()
        },
        color = LudoColor.RED.primaryColor
    )
    // Green (Top)
    drawPath(
        path = Path().apply {
            moveTo(centerLeft, centerTop)
            lineTo(centerRight, centerTop)
            lineTo(centerX, centerY)
            close()
        },
        color = LudoColor.GREEN.primaryColor
    )
    // Yellow (Right)
    drawPath(
        path = Path().apply {
            moveTo(centerRight, centerTop)
            lineTo(centerRight, centerBottom)
            lineTo(centerX, centerY)
            close()
        },
        color = LudoColor.YELLOW.primaryColor
    )
    // Blue (Bottom)
    drawPath(
        path = Path().apply {
            moveTo(centerLeft, centerBottom)
            lineTo(centerRight, centerBottom)
            lineTo(centerX, centerY)
            close()
        },
        color = LudoColor.BLUE.primaryColor
    )

    // Center Gold Crown Circle
    drawCircle(
        color = Color(0xFFFCD34D),
        radius = cellSize * 0.58f,
        center = Offset(centerX, centerY)
    )
    drawCircle(
        color = Color(0xFFB45309),
        radius = cellSize * 0.58f,
        center = Offset(centerX, centerY),
        style = Stroke(width = 2.5f)
    )
}

private fun DrawScope.drawYard(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    color: LudoColor,
    theme: BoardTheme
) {
    // Outer colored zone
    drawRect(color = color.primaryColor, topLeft = Offset(left, top), size = Size(width, height))

    // Inner white/card box with padding
    val pad = width * 0.16f
    val innerBoxW = width - pad * 2
    val innerBoxH = height - pad * 2
    val innerCol = if (theme == BoardTheme.ROYAL_DARK) Color(0xFF1E293B) else Color.White

    drawRect(
        color = innerCol,
        topLeft = Offset(left + pad, top + pad),
        size = Size(innerBoxW, innerBoxH)
    )

    // 4 Token sockets in yard
    val socketRadius = innerBoxW * 0.18f
    val socketOffsets = listOf(
        Offset(left + pad + innerBoxW * 0.3f, top + pad + innerBoxH * 0.3f),
        Offset(left + pad + innerBoxW * 0.7f, top + pad + innerBoxH * 0.3f),
        Offset(left + pad + innerBoxW * 0.3f, top + pad + innerBoxH * 0.7f),
        Offset(left + pad + innerBoxW * 0.7f, top + pad + innerBoxH * 0.7f)
    )

    socketOffsets.forEach { pos ->
        drawCircle(color = color.primaryColor.copy(alpha = 0.3f), radius = socketRadius, center = pos)
        drawCircle(color = color.primaryColor, radius = socketRadius, center = pos, style = Stroke(width = 2.5f))
    }
}

private fun DrawScope.drawStar(cx: Float, cy: Float, radius: Float, color: Color) {
    val path = Path()
    val innerRadius = radius * 0.45f
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) radius else innerRadius
        val angle = (i * 36 - 90) * Math.PI / 180.0
        val x = (cx + r * kotlin.math.cos(angle)).toFloat()
        val y = (cy + r * kotlin.math.sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path = path, color = color, style = Fill)
}
