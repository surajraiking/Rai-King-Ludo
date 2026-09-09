package com.example.data.model

enum class PowerUpType(
    val displayName: String,
    val description: String,
    val icon: String,
    val costCoins: Int
) {
    DIVINE_SHIELD(
        displayName = "Divine Shield",
        description = "Protects your token from being cut/captured for 1 round!",
        icon = "🛡️",
        costCoins = 300
    ),
    GOLDEN_DICE(
        displayName = "Lucky Sixer",
        description = "Empowers your next roll with guaranteed roll of 6!",
        icon = "🎲",
        costCoins = 500
    ),
    DOUBLE_BOOST(
        displayName = "Speed Boost",
        description = "Token jumps +2 bonus squares on its next move!",
        icon = "⚡",
        costCoins = 250
    ),
    FREEZE_SPELL(
        displayName = "Freeze Opponent",
        description = "Freezes an opponent's leading token for 1 turn!",
        icon = "❄️",
        costCoins = 400
    )
}

data class PowerUpInventory(
    val shields: Int = 2,
    val goldenDice: Int = 1,
    val speedBoosts: Int = 2,
    val freezeSpells: Int = 1
) {
    fun getCount(type: PowerUpType): Int = when (type) {
        PowerUpType.DIVINE_SHIELD -> shields
        PowerUpType.GOLDEN_DICE -> goldenDice
        PowerUpType.DOUBLE_BOOST -> speedBoosts
        PowerUpType.FREEZE_SPELL -> freezeSpells
    }

    fun consume(type: PowerUpType): PowerUpInventory = when (type) {
        PowerUpType.DIVINE_SHIELD -> copy(shields = (shields - 1).coerceAtLeast(0))
        PowerUpType.GOLDEN_DICE -> copy(goldenDice = (goldenDice - 1).coerceAtLeast(0))
        PowerUpType.DOUBLE_BOOST -> copy(speedBoosts = (speedBoosts - 1).coerceAtLeast(0))
        PowerUpType.FREEZE_SPELL -> copy(freezeSpells = (freezeSpells - 1).coerceAtLeast(0))
    }

    fun add(type: PowerUpType, amount: Int = 1): PowerUpInventory = when (type) {
        PowerUpType.DIVINE_SHIELD -> copy(shields = shields + amount)
        PowerUpType.GOLDEN_DICE -> copy(goldenDice = goldenDice + amount)
        PowerUpType.DOUBLE_BOOST -> copy(speedBoosts = speedBoosts + amount)
        PowerUpType.FREEZE_SPELL -> copy(freezeSpells = freezeSpells + amount)
    }
}

data class FortuneWheelSlot(
    val label: String,
    val rewardCoins: Int = 0,
    val powerUp: PowerUpType? = null,
    val colorHex: Long
)
