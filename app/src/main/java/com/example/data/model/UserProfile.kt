package com.example.data.model

data class UserProfile(
    val id: String = "user_suraj_rai",
    val username: String = "King Suraj",
    val avatarId: Int = 0,
    val coins: Long = 2500,
    val matchesPlayed: Int = 18,
    val matchesWon: Int = 14,
    val lastDailyRewardDate: Long = 0,
    val winStreak: Int = 3
) {
    val winRate: Int
        get() = if (matchesPlayed > 0) ((matchesWon.toDouble() / matchesPlayed) * 100).toInt() else 0

    val winLossRatio: String
        get() = "$matchesWon / ${matchesPlayed - matchesWon}"
}

data class AvatarOption(
    val id: Int,
    val name: String,
    val emoji: String,
    val backgroundHex: Long
)

val AVATAR_OPTIONS = listOf(
    AvatarOption(0, "King", "👑", 0xFFEAB308),
    AvatarOption(1, "Emperor", "🤴", 0xFF6366F1),
    AvatarOption(2, "Queen", "👸", 0xFFEC4899),
    AvatarOption(3, "Warrior", "⚔️", 0xFFEF4444),
    AvatarOption(4, "Wizard", "🧙‍♂️", 0xFF8B5CF6),
    AvatarOption(5, "Ninja", "🥷", 0xFF10B981),
    AvatarOption(6, "Tiger", "🐯", 0xFFF97316),
    AvatarOption(7, "Robot", "🤖", 0xFF06B6D4)
)

data class LeaderboardEntry(
    val rank: Int,
    val username: String,
    val avatarId: Int,
    val wins: Int,
    val coins: Long,
    val isCurrentUser: Boolean = false
)
