package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AVATAR_OPTIONS
import com.example.data.model.LeaderboardEntry
import com.example.data.model.UserProfile

@Composable
fun LeaderboardScreen(
    userProfile: UserProfile,
    onBack: () -> Unit
) {
    val leaderboardList = remember(userProfile) {
        listOf(
            LeaderboardEntry(1, "Suraj Rai", 0, 142, 28500, isCurrentUser = false),
            LeaderboardEntry(2, "Alex_Supreme", 1, 118, 22400, isCurrentUser = false),
            LeaderboardEntry(3, "LudoQueen", 2, 98, 19200, isCurrentUser = false),
            LeaderboardEntry(4, userProfile.username, userProfile.avatarId, userProfile.matchesWon, userProfile.coins, isCurrentUser = true),
            LeaderboardEntry(5, "DragonSlayer", 3, 76, 15300, isCurrentUser = false),
            LeaderboardEntry(6, "DiceWizard", 4, 64, 12800, isCurrentUser = false),
            LeaderboardEntry(7, "ShadowNinja", 5, 59, 11900, isCurrentUser = false),
            LeaderboardEntry(8, "GoldenTiger", 6, 51, 10200, isCurrentUser = false),
            LeaderboardEntry(9, "RoboChamp", 7, 44, 8900, isCurrentUser = false),
            LeaderboardEntry(10, "LuckyStriker", 1, 38, 7600, isCurrentUser = false)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                )
            )
            .testTag("leaderboard_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("leaderboard_back_button")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "🏆 Global Leaderboard",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFBBF24),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Weekly Champion Hero Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF59E0B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "👑", fontSize = 28.sp)
                    }
                    Column {
                        Text(
                            text = "WEEKLY TOURNAMENT KING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFBBF24),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Suraj Rai",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "142 Wins • 28,500 Coins",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Rankings List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(leaderboardList) { entry ->
                    val isTop3 = entry.rank <= 3
                    val avatar = AVATAR_OPTIONS.getOrNull(entry.avatarId) ?: AVATAR_OPTIONS[0]

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (entry.isCurrentUser) Color(0xFF1E3A8A) else Color(0xFF1E293B)
                        ),
                        border = if (entry.isCurrentUser) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF60A5FA)) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Rank Badge
                            Box(
                                modifier = Modifier.size(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                when (entry.rank) {
                                    1 -> Text(text = "🥇", fontSize = 22.sp)
                                    2 -> Text(text = "🥈", fontSize = 22.sp)
                                    3 -> Text(text = "🥉", fontSize = 22.sp)
                                    else -> Text(
                                        text = "#${entry.rank}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(avatar.backgroundHex)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = avatar.emoji, fontSize = 20.sp)
                            }

                            // Username
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = entry.username,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (entry.isCurrentUser) Color(0xFF93C5FD) else Color.White
                                    )
                                    if (entry.isCurrentUser) {
                                        Text(
                                            text = " (You)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF60A5FA)
                                        )
                                    }
                                }
                                Text(
                                    text = "${entry.wins} Wins",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            // Coins
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(text = "🪙", fontSize = 14.sp)
                                Text(
                                    text = "%,d".format(entry.coins),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFBBF24)
                                )
                            }
                        }
                    }
                }
            }

            // Footer branding
            Text(
                text = "Rai Ludo King • Developed by Suraj Rai",
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 6.dp)
            )
        }
    }
}
