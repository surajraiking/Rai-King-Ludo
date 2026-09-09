package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AVATAR_OPTIONS
import com.example.data.model.UserProfile

@Composable
fun HomeScreen(
    userProfile: UserProfile,
    onPlayOnline: () -> Unit,
    onPlayWithFriends: () -> Unit,
    onPlayWithBot: () -> Unit,
    onLocalPlay: () -> Unit,
    onOpenSearchArena: () -> Unit = {},
    onOpenLiveVoice: () -> Unit = {},
    onOpenFortuneWheel: () -> Unit = {},
    onOpenLeaderboard: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDailyReward: () -> Unit
) {
    val currentAvatar = AVATAR_OPTIONS.getOrNull(userProfile.avatarId) ?: AVATAR_OPTIONS[0]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B),
                        Color(0xFF0F172A)
                    )
                )
            )
            .testTag("home_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top Header: Profile Info & Coins & Daily Reward
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Avatar & Nickname (clickable to profile)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E293B))
                        .clickable { onOpenProfile() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("home_profile_chip")
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(currentAvatar.backgroundHex)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = currentAvatar.emoji, fontSize = 18.sp)
                    }
                    Column {
                        Text(
                            text = userProfile.username,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Lv. ${(userProfile.matchesWon / 3) + 1}",
                            fontSize = 10.sp,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Coins Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFF59E0B).copy(alpha = 0.2f))
                            .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "🪙", fontSize = 14.sp)
                            Text(
                                text = "%,d".format(userProfile.coins),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFBBF24)
                            )
                        }
                    }

                    // Daily Bonus Gift Button
                    IconButton(
                        onClick = onOpenDailyReward,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                            .testTag("daily_bonus_button")
                    ) {
                        Text(text = "🎁", fontSize = 18.sp)
                    }

                    // Lucky Spin Wheel Button
                    IconButton(
                        onClick = onOpenFortuneWheel,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF8B5CF6))
                            .testTag("home_fortune_wheel_button")
                    ) {
                        Text(text = "🎡", fontSize = 18.sp)
                    }

                    // Settings Button
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF334155))
                            .testTag("home_settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hero Branding Title Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B).copy(alpha = 0.85f)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = "👑", fontSize = 26.sp)
                            Text(
                                text = "RAI LUDO KING",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFBBF24),
                                letterSpacing = 2.sp
                            )
                            Text(text = "🎲", fontSize = 26.sp)
                        }
                        Text(
                            text = "Real-Time Online Multiplayer & Pass and Play",
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1),
                            textAlign = TextAlign.Center
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Developed by Suraj Rai",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }

                // Main Game Mode Selection Cards
                GameModeCard(
                    title = "Sanatan KBC Maha-Quiz",
                    subtitle = "Hard Vedic Trivia Grounded with Google Search • 4 Lifelines • Win 🪙 Crores",
                    badge = "🕉️ SANATAN KBC ARENA",
                    icon = Icons.Default.AutoAwesome,
                    gradientColors = listOf(Color(0xFFD97706), Color(0xFFB45309)),
                    onClick = onOpenSearchArena,
                    testTag = "mode_search_arena"
                )

                GameModeCard(
                    title = "Rai AI Voice Grandmaster",
                    subtitle = "Live Voice Companion (gemini-3.1-flash-live-preview) • Talk tactics",
                    badge = "🎙️ LIVE VOICE API",
                    icon = Icons.Default.Mic,
                    gradientColors = listOf(Color(0xFFD97706), Color(0xFFB45309)),
                    onClick = onOpenLiveVoice,
                    testTag = "mode_live_voice"
                )

                GameModeCard(
                    title = "Play Online",
                    subtitle = "Quick Match with global players in real-time",
                    badge = "⚡ QUICK MATCH",
                    icon = Icons.Default.Wifi,
                    gradientColors = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)),
                    onClick = onPlayOnline,
                    testTag = "mode_play_online"
                )

                GameModeCard(
                    title = "Play with Friends",
                    subtitle = "Create or Join a private room with 6-digit code",
                    badge = "👥 PRIVATE ROOM",
                    icon = Icons.Default.Group,
                    gradientColors = listOf(Color(0xFF7C3AED), Color(0xFF6D28D9)),
                    onClick = onPlayWithFriends,
                    testTag = "mode_play_friends"
                )

                GameModeCard(
                    title = "Play with Computer / Bot",
                    subtitle = "Challenge smart tactical AI bots offline",
                    badge = "🤖 OFFLINE AI",
                    icon = Icons.Default.SmartToy,
                    gradientColors = listOf(Color(0xFF059669), Color(0xFF047857)),
                    onClick = onPlayWithBot,
                    testTag = "mode_play_bot"
                )

                GameModeCard(
                    title = "Local Pass & Play",
                    subtitle = "Pass device between 2 to 4 friends",
                    badge = "🎲 LOCAL MULTIPLAYER",
                    icon = Icons.Default.PlayArrow,
                    gradientColors = listOf(Color(0xFFEA580C), Color(0xFFC2410C)),
                    onClick = onLocalPlay,
                    testTag = "mode_local_play"
                )

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Bottom Navigation Bar
            NavigationBar(
                containerColor = Color(0xFF1E293B),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Home") },
                    label = { Text("Play", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFFBBF24),
                        selectedTextColor = Color(0xFFFBBF24),
                        indicatorColor = Color(0xFF334155)
                    )
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onOpenLeaderboard,
                    icon = { Icon(Icons.Default.Leaderboard, contentDescription = "Leaderboard") },
                    label = { Text("Leaderboard") },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onOpenProfile,
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )

                NavigationBarItem(
                    selected = false,
                    onClick = onOpenSettings,
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    )
                )
            }

            // Footer developer attribution
            Text(
                text = "Rai Ludo King • Developed by Suraj Rai",
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun GameModeCard(
    title: String,
    subtitle: String,
    badge: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = Brush.horizontalGradient(gradientColors))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(28.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFDE047),
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )

                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                Text(text = "▶", fontSize = 20.sp, color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}
