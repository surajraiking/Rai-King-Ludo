package com.example.ai

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat

@Composable
fun GeminiLiveVoiceDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val liveVoiceManager = remember { GeminiLiveVoiceManager(context, scope) }

    var textInput by remember { mutableStateOf("") }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        liveVoiceManager.connectLiveSession()
    }

    val voiceState by liveVoiceManager.voiceState.collectAsState()
    val connectionState = voiceState.connectionState
    val isMicMuted = voiceState.isMicMuted
    val isAiSpeaking = connectionState == LiveConnectionState.SPEAKING
    val isConnected = connectionState == LiveConnectionState.CONNECTED ||
            connectionState == LiveConnectionState.LISTENING ||
            connectionState == LiveConnectionState.SPEAKING

    DisposableEffect(Unit) {
        onDispose {
            liveVoiceManager.disconnect()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isAiSpeaking || isConnected) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                Brush.horizontalGradient(listOf(Color(0xFFD97706), Color(0xFFFBBF24), Color(0xFF6366F1)))
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("gemini_live_voice_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🎙️ Rai AI Voice Grandmaster",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = "Bidirectional Voice (gemini-3.1-flash-live-preview)",
                            fontSize = 10.sp,
                            color = Color(0xFFFBBF24)
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                    }
                }

                // Central Animated Sphere
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = when (connectionState) {
                                    LiveConnectionState.SPEAKING -> listOf(Color(0xFFF59E0B), Color(0xFFD97706), Color(0xFF78350F))
                                    LiveConnectionState.LISTENING, LiveConnectionState.CONNECTED -> listOf(Color(0xFF10B981), Color(0xFF059669), Color(0xFF064E3B))
                                    LiveConnectionState.CONNECTING -> listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8), Color(0xFF1E3A8A))
                                    else -> listOf(Color(0xFF475569), Color(0xFF334155), Color(0xFF1E293B))
                                }
                            )
                        )
                        .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (connectionState == LiveConnectionState.CONNECTING) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(34.dp), strokeWidth = 3.dp)
                    } else {
                        Icon(
                            imageVector = if (isAiSpeaking) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.Mic,
                            contentDescription = "Audio State",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                // Status Indicator
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = when (connectionState) {
                            LiveConnectionState.SPEAKING -> "🔊 AI Grandmaster speaking (Aoede voice)..."
                            LiveConnectionState.LISTENING -> "🟢 Listening live to your microphone..."
                            LiveConnectionState.CONNECTED -> "🟢 Connected to Gemini Live Voice!"
                            LiveConnectionState.CONNECTING -> "🟡 Connecting to WebSocket..."
                            LiveConnectionState.ERROR -> "🔴 Connection Error"
                            LiveConnectionState.DISCONNECTED -> "⚪ Offline / Ready to Connect"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (connectionState) {
                            LiveConnectionState.SPEAKING, LiveConnectionState.LISTENING, LiveConnectionState.CONNECTED -> Color(0xFF34D399)
                            LiveConnectionState.CONNECTING -> Color(0xFFFBBF24)
                            LiveConnectionState.ERROR -> Color(0xFFF87171)
                            LiveConnectionState.DISCONNECTED -> Color(0xFF94A3B8)
                        },
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = voiceState.statusMessage,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // Recent dialogue transcript
                if (voiceState.messageHistory.isNotEmpty()) {
                    val lastMsg = voiceState.messageHistory.last()
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "${lastMsg.sender}:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (lastMsg.isUser) Color(0xFF38BDF8) else Color(0xFFFBBF24)
                            )
                            Text(
                                text = lastMsg.text,
                                fontSize = 12.sp,
                                color = Color.White,
                                maxLines = 3
                            )
                        }
                    }
                }

                // Suggested Prompts
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E293B))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "💡 Tap or Speak Question:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )

                    listOf(
                        "What should I move after rolling a 6?",
                        "When is it safest to deploy Divine Shield?",
                        "How do I trap my opponent in Ludo?"
                    ).forEach { prompt ->
                        Text(
                            text = "• $prompt",
                            fontSize = 10.sp,
                            color = Color(0xFFCBD5E1),
                            modifier = Modifier
                                .clickable {
                                    liveVoiceManager.sendTextMessage(prompt)
                                }
                                .padding(vertical = 2.dp)
                        )
                    }
                }

                // Action Controls
                if (isConnected) {
                    // Text query row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("Ask strategy or question...", fontSize = 12.sp, color = Color.Gray) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("voice_chat_text_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFF59E0B),
                                unfocusedBorderColor = Color(0xFF475569),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )

                        IconButton(
                            onClick = {
                                if (textInput.isNotBlank()) {
                                    liveVoiceManager.sendTextMessage(textInput)
                                    textInput = ""
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B))
                                .testTag("voice_chat_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color(0xFF0F172A)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mic Mute / Unmute Button
                        Button(
                            onClick = {
                                if (!hasAudioPermission) {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    liveVoiceManager.toggleMicMute()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isMicMuted && hasAudioPermission) Color(0xFF10B981) else Color(0xFFEF4444)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (!isMicMuted && hasAudioPermission) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = "Mic",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (!hasAudioPermission) "Enable Mic" else if (!isMicMuted) "Mute" else "Unmute",
                                fontSize = 12.sp
                            )
                        }

                        // Disconnect Button
                        Button(
                            onClick = { liveVoiceManager.disconnect() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhoneDisabled, contentDescription = "End Call", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("End Call", fontSize = 12.sp)
                        }
                    }
                } else {
                    // Connect Button
                    Button(
                        onClick = {
                            val permissionCheck = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (permissionCheck) {
                                hasAudioPermission = true
                                liveVoiceManager.connectLiveSession()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        enabled = connectionState != LiveConnectionState.CONNECTING,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("start_live_voice_button")
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Call AI", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Live Voice Call",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
