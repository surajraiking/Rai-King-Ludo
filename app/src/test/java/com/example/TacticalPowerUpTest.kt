package com.example

import com.example.data.model.LudoColor
import com.example.data.model.PowerUpInventory
import com.example.data.model.PowerUpType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TacticalPowerUpTest {

    @Test
    fun `test power up inventory initial counts and consumption`() {
        val inventory = PowerUpInventory(
            shields = 2,
            goldenDice = 1,
            speedBoosts = 2,
            freezeSpells = 1
        )

        assertEquals(2, inventory.getCount(PowerUpType.DIVINE_SHIELD))
        assertEquals(1, inventory.getCount(PowerUpType.GOLDEN_DICE))
        assertEquals(2, inventory.getCount(PowerUpType.DOUBLE_BOOST))
        assertEquals(1, inventory.getCount(PowerUpType.FREEZE_SPELL))

        // Consume Divine Shield
        val afterShield = inventory.consume(PowerUpType.DIVINE_SHIELD)
        assertEquals(1, afterShield.shields)
        assertEquals(1, afterShield.goldenDice)

        // Consume Golden Dice
        val afterDice = afterShield.consume(PowerUpType.GOLDEN_DICE)
        assertEquals(0, afterDice.goldenDice)

        // Try consuming empty
        val afterEmptyDice = afterDice.consume(PowerUpType.GOLDEN_DICE)
        assertEquals(0, afterEmptyDice.goldenDice)

        // Add back from Fortune Wheel
        val afterAdd = afterEmptyDice.add(PowerUpType.GOLDEN_DICE, 3)
        assertEquals(3, afterAdd.goldenDice)
    }

    @Test
    fun `test power up types metadata`() {
        assertEquals("Divine Shield", PowerUpType.DIVINE_SHIELD.displayName)
        assertEquals("🛡️", PowerUpType.DIVINE_SHIELD.icon)

        assertEquals("Lucky Sixer", PowerUpType.GOLDEN_DICE.displayName)
        assertEquals("🎲", PowerUpType.GOLDEN_DICE.icon)

        assertEquals("Speed Boost", PowerUpType.DOUBLE_BOOST.displayName)
        assertEquals("⚡", PowerUpType.DOUBLE_BOOST.icon)

        assertEquals("Freeze Opponent", PowerUpType.FREEZE_SPELL.displayName)
        assertEquals("❄️", PowerUpType.FREEZE_SPELL.icon)
    }
}
