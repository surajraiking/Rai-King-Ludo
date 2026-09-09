package com.example.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.BoardTheme
import com.example.data.model.PowerUpInventory
import com.example.data.model.PowerUpType
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("rai_ludo_king_prefs", Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow(loadUserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _powerUpInventory = MutableStateFlow(loadPowerUpInventory())
    val powerUpInventory: StateFlow<PowerUpInventory> = _powerUpInventory.asStateFlow()

    private val _soundEnabled = MutableStateFlow(prefs.getBoolean(KEY_SOUND, true))
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(prefs.getBoolean(KEY_VIBRATION, true))
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    private val _boardTheme = MutableStateFlow(
        BoardTheme.valueOf(prefs.getString(KEY_THEME, BoardTheme.CLASSIC.name) ?: BoardTheme.CLASSIC.name)
    )
    val boardTheme: StateFlow<BoardTheme> = _boardTheme.asStateFlow()

    private fun loadPowerUpInventory(): PowerUpInventory {
        return PowerUpInventory(
            shields = prefs.getInt(KEY_SHIELDS, 2),
            goldenDice = prefs.getInt(KEY_GOLDEN_DICE, 1),
            speedBoosts = prefs.getInt(KEY_SPEED_BOOSTS, 2),
            freezeSpells = prefs.getInt(KEY_FREEZE_SPELLS, 1)
        )
    }

    fun addPowerUp(type: PowerUpType, amount: Int = 1) {
        val updated = _powerUpInventory.value.add(type, amount)
        _powerUpInventory.value = updated
        savePowerUpInventory(updated)
    }

    fun consumePowerUp(type: PowerUpType): Boolean {
        if (_powerUpInventory.value.getCount(type) <= 0) return false
        val updated = _powerUpInventory.value.consume(type)
        _powerUpInventory.value = updated
        savePowerUpInventory(updated)
        return true
    }

    private fun savePowerUpInventory(inv: PowerUpInventory) {
        prefs.edit()
            .putInt(KEY_SHIELDS, inv.shields)
            .putInt(KEY_GOLDEN_DICE, inv.goldenDice)
            .putInt(KEY_SPEED_BOOSTS, inv.speedBoosts)
            .putInt(KEY_FREEZE_SPELLS, inv.freezeSpells)
            .apply()
    }

    private fun loadUserProfile(): UserProfile {
        return UserProfile(
            id = prefs.getString(KEY_USER_ID, "suraj_player_1") ?: "suraj_player_1",
            username = prefs.getString(KEY_USERNAME, "King Suraj") ?: "King Suraj",
            avatarId = prefs.getInt(KEY_AVATAR_ID, 0),
            coins = prefs.getLong(KEY_COINS, 2500L),
            matchesPlayed = prefs.getInt(KEY_MATCHES_PLAYED, 18),
            matchesWon = prefs.getInt(KEY_MATCHES_WON, 14),
            lastDailyRewardDate = prefs.getLong(KEY_LAST_DAILY, 0L),
            winStreak = prefs.getInt(KEY_WIN_STREAK, 3)
        )
    }

    fun updateProfile(username: String, avatarId: Int) {
        val updated = _userProfile.value.copy(username = username, avatarId = avatarId)
        _userProfile.value = updated
        prefs.edit()
            .putString(KEY_USERNAME, username)
            .putInt(KEY_AVATAR_ID, avatarId)
            .apply()
    }

    fun addCoins(amount: Long) {
        val updated = _userProfile.value.copy(coins = (_userProfile.value.coins + amount).coerceAtLeast(0))
        _userProfile.value = updated
        prefs.edit().putLong(KEY_COINS, updated.coins).apply()
    }

    fun recordMatchResult(won: Boolean, coinsEarned: Long) {
        val current = _userProfile.value
        val newPlayed = current.matchesPlayed + 1
        val newWon = if (won) current.matchesWon + 1 else current.matchesWon
        val newStreak = if (won) current.winStreak + 1 else 0
        val newCoins = (current.coins + coinsEarned).coerceAtLeast(0)

        val updated = current.copy(
            matchesPlayed = newPlayed,
            matchesWon = newWon,
            winStreak = newStreak,
            coins = newCoins
        )
        _userProfile.value = updated
        prefs.edit()
            .putInt(KEY_MATCHES_PLAYED, newPlayed)
            .putInt(KEY_MATCHES_WON, newWon)
            .putInt(KEY_WIN_STREAK, newStreak)
            .putLong(KEY_COINS, newCoins)
            .apply()
    }

    fun claimDailyReward(rewardCoins: Long): Boolean {
        val current = _userProfile.value
        val today = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
        val lastClaimDay = current.lastDailyRewardDate / (1000 * 60 * 60 * 24)

        if (today > lastClaimDay) {
            val updated = current.copy(
                coins = current.coins + rewardCoins,
                lastDailyRewardDate = System.currentTimeMillis()
            )
            _userProfile.value = updated
            prefs.edit()
                .putLong(KEY_COINS, updated.coins)
                .putLong(KEY_LAST_DAILY, updated.lastDailyRewardDate)
                .apply()
            return true
        }
        return false
    }

    fun setSoundEnabled(enabled: Boolean) {
        _soundEnabled.value = enabled
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply()
    }

    fun setVibrationEnabled(enabled: Boolean) {
        _vibrationEnabled.value = enabled
        prefs.edit().putBoolean(KEY_VIBRATION, enabled).apply()
    }

    fun setBoardTheme(theme: BoardTheme) {
        _boardTheme.value = theme
        prefs.edit().putString(KEY_THEME, theme.name).apply()
    }

    companion object {
        private const val KEY_USER_ID = "key_user_id"
        private const val KEY_USERNAME = "key_username"
        private const val KEY_AVATAR_ID = "key_avatar_id"
        private const val KEY_COINS = "key_coins"
        private const val KEY_MATCHES_PLAYED = "key_matches_played"
        private const val KEY_MATCHES_WON = "key_matches_won"
        private const val KEY_LAST_DAILY = "key_last_daily"
        private const val KEY_WIN_STREAK = "key_win_streak"
        private const val KEY_SOUND = "key_sound"
        private const val KEY_VIBRATION = "key_vibration"
        private const val KEY_THEME = "key_theme"
        private const val KEY_SHIELDS = "key_shields"
        private const val KEY_GOLDEN_DICE = "key_golden_dice"
        private const val KEY_SPEED_BOOSTS = "key_speed_boosts"
        private const val KEY_FREEZE_SPELLS = "key_freeze_spells"
    }
}
