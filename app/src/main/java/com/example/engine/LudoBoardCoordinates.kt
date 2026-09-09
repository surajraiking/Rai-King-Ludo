package com.example.engine

import androidx.compose.ui.geometry.Offset
import com.example.data.model.LudoColor

data class GridCoord(val col: Float, val row: Float)

object LudoBoardCoordinates {

    // 52 cells on the perimeter/track
    val TRACK_CELLS: List<GridCoord> = listOf(
        // 0..4 (Red arm going right)
        GridCoord(1f, 6f), // 0: Red Start (Safe)
        GridCoord(2f, 6f),
        GridCoord(3f, 6f),
        GridCoord(4f, 6f),
        GridCoord(5f, 6f),
        // 5..10 (Green vertical arm going up)
        GridCoord(6f, 5f),
        GridCoord(6f, 4f),
        GridCoord(6f, 3f),
        GridCoord(6f, 2f), // 8: Star Safe
        GridCoord(6f, 1f),
        GridCoord(6f, 0f),
        // 11..12 (Top turn)
        GridCoord(7f, 0f),
        GridCoord(8f, 0f),
        // 13..17 (Green arm going down)
        GridCoord(8f, 1f), // 13: Green Start (Safe)
        GridCoord(8f, 2f),
        GridCoord(8f, 3f),
        GridCoord(8f, 4f),
        GridCoord(8f, 5f),
        // 18..23 (Yellow horizontal arm going right)
        GridCoord(9f, 6f),
        GridCoord(10f, 6f),
        GridCoord(11f, 6f),
        GridCoord(12f, 6f), // 21: Star Safe
        GridCoord(13f, 6f),
        GridCoord(14f, 6f),
        // 24..25 (Right turn)
        GridCoord(14f, 7f),
        GridCoord(14f, 8f),
        // 26..30 (Yellow horizontal arm going left)
        GridCoord(13f, 8f), // 26: Yellow Start (Safe)
        GridCoord(12f, 8f),
        GridCoord(11f, 8f),
        GridCoord(10f, 8f),
        GridCoord(9f, 8f),
        // 31..36 (Blue vertical arm going down)
        GridCoord(8f, 9f),
        GridCoord(8f, 10f),
        GridCoord(8f, 11f),
        GridCoord(8f, 12f), // 34: Star Safe
        GridCoord(8f, 13f),
        GridCoord(8f, 14f),
        // 37..38 (Bottom turn)
        GridCoord(7f, 14f),
        GridCoord(6f, 14f),
        // 39..43 (Blue vertical arm going up)
        GridCoord(6f, 13f), // 39: Blue Start (Safe)
        GridCoord(6f, 12f),
        GridCoord(6f, 11f),
        GridCoord(6f, 10f),
        GridCoord(6f, 9f),
        // 44..49 (Red horizontal arm going left)
        GridCoord(5f, 8f),
        GridCoord(4f, 8f),
        GridCoord(3f, 8f),
        GridCoord(2f, 8f), // 47: Star Safe
        GridCoord(1f, 8f),
        GridCoord(0f, 8f),
        // 50..51 (Left turn)
        GridCoord(0f, 7f),
        GridCoord(0f, 6f)
    )

    val SAFE_TRACK_INDICES: Set<Int> = setOf(0, 8, 13, 21, 26, 34, 39, 47)

    // Home corridor cells for each color (steps 51..55)
    val RED_HOME_CORRIDOR = listOf(
        GridCoord(1f, 7f),
        GridCoord(2f, 7f),
        GridCoord(3f, 7f),
        GridCoord(4f, 7f),
        GridCoord(5f, 7f)
    )

    val GREEN_HOME_CORRIDOR = listOf(
        GridCoord(7f, 1f),
        GridCoord(7f, 2f),
        GridCoord(7f, 3f),
        GridCoord(7f, 4f),
        GridCoord(7f, 5f)
    )

    val YELLOW_HOME_CORRIDOR = listOf(
        GridCoord(13f, 7f),
        GridCoord(12f, 7f),
        GridCoord(11f, 7f),
        GridCoord(10f, 7f),
        GridCoord(9f, 7f)
    )

    val BLUE_HOME_CORRIDOR = listOf(
        GridCoord(7f, 13f),
        GridCoord(7f, 12f),
        GridCoord(7f, 11f),
        GridCoord(7f, 10f),
        GridCoord(7f, 9f)
    )

    // Yard positions (row/col) for the 4 tokens of each player
    fun getYardCoord(color: LudoColor, tokenId: Int): GridCoord {
        val (baseCol, baseRow) = when (color) {
            LudoColor.RED -> Pair(1.5f, 1.5f)
            LudoColor.GREEN -> Pair(10.5f, 1.5f)
            LudoColor.YELLOW -> Pair(10.5f, 10.5f)
            LudoColor.BLUE -> Pair(1.5f, 10.5f)
        }
        val offsetX = if (tokenId % 2 == 1) 2.0f else 0f
        val offsetY = if (tokenId >= 2) 2.0f else 0f
        return GridCoord(baseCol + offsetX, baseRow + offsetY)
    }

    // Home center position (step 56)
    fun getHomeCenterCoord(color: LudoColor, tokenId: Int): GridCoord {
        val center = 7.0f
        val offset = 0.35f
        return when (color) {
            LudoColor.RED -> GridCoord(center - 0.7f, center + (tokenId - 1.5f) * offset)
            LudoColor.GREEN -> GridCoord(center + (tokenId - 1.5f) * offset, center - 0.7f)
            LudoColor.YELLOW -> GridCoord(center + 0.7f, center + (tokenId - 1.5f) * offset)
            LudoColor.BLUE -> GridCoord(center + (tokenId - 1.5f) * offset, center + 0.7f)
        }
    }

    fun getTokenGridCoord(color: LudoColor, tokenId: Int, stepCount: Int): GridCoord {
        if (stepCount == -1) {
            return getYardCoord(color, tokenId)
        }
        if (stepCount == 56) {
            return getHomeCenterCoord(color, tokenId)
        }
        if (stepCount in 0..50) {
            val globalIdx = (color.startTrackIndex + stepCount) % 52
            return TRACK_CELLS[globalIdx]
        }
        // Home corridor (steps 51..55)
        val corridorIdx = (stepCount - 51).coerceIn(0, 4)
        return when (color) {
            LudoColor.RED -> RED_HOME_CORRIDOR[corridorIdx]
            LudoColor.GREEN -> GREEN_HOME_CORRIDOR[corridorIdx]
            LudoColor.YELLOW -> YELLOW_HOME_CORRIDOR[corridorIdx]
            LudoColor.BLUE -> BLUE_HOME_CORRIDOR[corridorIdx]
        }
    }

    fun isSafeCell(globalTrackIndex: Int): Boolean {
        return globalTrackIndex in SAFE_TRACK_INDICES
    }
}
