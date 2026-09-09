package com.example

import com.example.data.model.LudoColor
import com.example.data.model.LudoDiceState
import com.example.ui.viewmodel.LudoDiceViewModel
import com.example.ui.viewmodel.PlayerDiceConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class LudoDiceTest {

    @Test
    fun `ludo dice state default values and properties`() {
        val state = LudoDiceState()

        assertEquals(1, state.value)
        assertFalse(state.isRolling)
        assertFalse(state.hasRolled)
        assertEquals(0, state.activePlayerIndex)
        assertEquals(LudoColor.RED, state.activePlayerColor)
        assertEquals("Player 1", state.activePlayerName)
        assertEquals(0, state.consecutiveSixes)
        assertFalse(state.isThreeSixesPenalty)
        assertFalse(state.isBonusRoll)
        assertTrue(state.canRoll)
        assertFalse(state.isSix)
        assertFalse(state.qualifiesForBonusRoll)
    }

    @Test
    fun `ludo dice state canRoll constraints`() {
        val rollingState = LudoDiceState(isRolling = true)
        assertFalse(rollingState.canRoll)

        val rolledState = LudoDiceState(hasRolled = true)
        assertFalse(rolledState.canRoll)

        val penaltyState = LudoDiceState(isThreeSixesPenalty = true)
        assertFalse(penaltyState.canRoll)

        val sixState = LudoDiceState(value = 6, consecutiveSixes = 1)
        assertTrue(sixState.isSix)
        assertTrue(sixState.qualifiesForBonusRoll)

        val threeSixState = LudoDiceState(value = 6, consecutiveSixes = 3)
        assertTrue(threeSixState.isSix)
        assertFalse(threeSixState.qualifiesForBonusRoll)
    }

    @Test
    fun `viewmodel player setup and turn passing`() = runTest {
        val customPlayers = listOf(
            PlayerDiceConfig("Alice", LudoColor.RED),
            PlayerDiceConfig("Bob", LudoColor.GREEN),
            PlayerDiceConfig("Charlie", LudoColor.YELLOW)
        )

        val viewModel = LudoDiceViewModel(externalScope = backgroundScope)
        viewModel.setupPlayers(customPlayers)

        var state = viewModel.diceState.value
        assertEquals("Alice", state.activePlayerName)
        assertEquals(0, state.activePlayerIndex)
        assertEquals(LudoColor.RED, state.activePlayerColor)
        assertTrue(state.canRoll)

        // Pass turn to Bob
        viewModel.passTurn()
        state = viewModel.diceState.value
        assertEquals("Bob", state.activePlayerName)
        assertEquals(1, state.activePlayerIndex)
        assertEquals(LudoColor.GREEN, state.activePlayerColor)
        assertEquals(1, state.totalTurnsCompleted)
        assertTrue(state.canRoll)

        // Pass turn to Charlie
        viewModel.passTurn()
        state = viewModel.diceState.value
        assertEquals("Charlie", state.activePlayerName)
        assertEquals(2, state.activePlayerIndex)
        assertEquals(LudoColor.YELLOW, state.activePlayerColor)

        // Wrap around back to Alice
        viewModel.passTurn()
        state = viewModel.diceState.value
        assertEquals("Alice", state.activePlayerName)
        assertEquals(0, state.activePlayerIndex)
    }

    @Test
    fun `viewmodel roll dice with deterministic random generator`() = runTest {
        // Deterministic random returning 6
        val deterministicRandom = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextInt(from: Int, until: Int): Int = 6
        }

        val viewModel = LudoDiceViewModel(randomSource = deterministicRandom, externalScope = backgroundScope)

        var callbackRoll = 0
        viewModel.rollDice { roll ->
            callbackRoll = roll
        }

        // Advance past animation frames (7 frames * 50ms)
        advanceTimeBy(1000L)

        val state = viewModel.diceState.value
        assertEquals(6, state.value)
        assertEquals(6, callbackRoll)
        assertTrue(state.hasRolled)
        assertFalse(state.isRolling)
        assertTrue(state.isBonusRoll)
        assertEquals(1, state.consecutiveSixes)
        assertEquals(listOf(6), state.rollHistory)
    }

    @Test
    fun `viewmodel bonus turn on roll 6 and token movement`() = runTest {
        val deterministicRandom = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextInt(from: Int, until: Int): Int = 6
        }

        val viewModel = LudoDiceViewModel(randomSource = deterministicRandom, externalScope = backgroundScope)
        viewModel.rollDice()
        advanceTimeBy(1000L)

        var state = viewModel.diceState.value
        assertTrue(state.hasRolled)
        assertTrue(state.isBonusRoll)
        val initialPlayerIndex = state.activePlayerIndex

        // Complete token move, which triggers bonus turn
        viewModel.onMoveCompleted(hasAdditionalBonus = false)
        state = viewModel.diceState.value

        // Should retain the same active player and allow them to roll again
        assertEquals(initialPlayerIndex, state.activePlayerIndex)
        assertFalse(state.hasRolled)
        assertTrue(state.canRoll)
    }

    @Test
    fun `viewmodel forfeits turn on three consecutive sixes`() = runTest {
        val deterministicRandom = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextInt(from: Int, until: Int): Int = 6
        }

        val viewModel = LudoDiceViewModel(randomSource = deterministicRandom, externalScope = backgroundScope)

        // Roll 1: Six
        viewModel.rollDice()
        advanceTimeBy(1000L)
        viewModel.onMoveCompleted()

        // Roll 2: Six
        viewModel.rollDice()
        advanceTimeBy(1000L)
        viewModel.onMoveCompleted()

        // Roll 3: Six (Should trigger penalty and forfeit turn)
        viewModel.rollDice()
        advanceTimeBy(500L)
        assertTrue(viewModel.diceState.value.isThreeSixesPenalty)

        // Advance past penalty delay (1200ms)
        advanceTimeBy(1500L)
        // Turn should have passed to player 1
        assertEquals(1, viewModel.diceState.value.activePlayerIndex)
        assertFalse(viewModel.diceState.value.isThreeSixesPenalty)
    }
}
