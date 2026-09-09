package com.example.ai

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class LiveConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    LISTENING,
    SPEAKING,
    ERROR
}

data class LiveVoiceMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class LiveVoiceState(
    val connectionState: LiveConnectionState = LiveConnectionState.DISCONNECTED,
    val isMicMuted: Boolean = false,
    val audioAmplitude: Float = 0f,
    val currentAiResponse: String = "",
    val messageHistory: List<LiveVoiceMessage> = emptyList(),
    val statusMessage: String = "Ready to connect with Rai AI Grandmaster"
)

/**
 * Manages real-time bidirectional voice conversations with gemini-3.1-flash-live-preview
 * using WebSockets, audio streaming, and live speech transcription.
 */
class GeminiLiveVoiceManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _voiceState = MutableStateFlow(LiveVoiceState())
    val voiceState: StateFlow<LiveVoiceState> = _voiceState.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Keep-alive for WebSocket
        .build()

    private var webSocket: WebSocket? = null
    private var recordingJob: Job? = null
    private var audioTrack: AudioTrack? = null
    private var audioRecord: AudioRecord? = null

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    fun connectLiveSession() {
        val currentState = _voiceState.value.connectionState
        if (currentState == LiveConnectionState.CONNECTING || currentState == LiveConnectionState.CONNECTED) {
            return
        }

        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Graceful offline/simulation mode
            startSimulatedLiveSession("Connecting in Grandmaster offline mode. Ready for voice interaction!")
            return
        }

        _voiceState.update {
            it.copy(
                connectionState = LiveConnectionState.CONNECTING,
                statusMessage = "Connecting to gemini-3.1-flash-live-preview..."
            )
        }

        val wsUrl = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey"
        val request = Request.Builder().url(wsUrl).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("GeminiLiveVoice", "WebSocket opened successfully")
                sendSetupMessage(webSocket)
                _voiceState.update {
                    it.copy(
                        connectionState = LiveConnectionState.CONNECTED,
                        statusMessage = "Connected to Rai AI Live Grandmaster!"
                    )
                }
                startMicrophoneStreaming()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleServerMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w("GeminiLiveVoice", "WebSocket failed: ${t.message}")
                scope.launch(Dispatchers.Main) {
                    startSimulatedLiveSession("Live server fallback active. Grandmaster is listening!")
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("GeminiLiveVoice", "WebSocket closed: $reason")
                _voiceState.update {
                    it.copy(
                        connectionState = LiveConnectionState.DISCONNECTED,
                        statusMessage = "Session disconnected"
                    )
                }
                stopMicrophoneStreaming()
            }
        })
    }

    private fun sendSetupMessage(ws: WebSocket) {
        val setupPayload = JSONObject().apply {
            val setupObj = JSONObject().apply {
                put("model", "models/gemini-3.1-flash-live-preview")
                val genConfig = JSONObject().apply {
                    val modalities = JSONArray().apply {
                        put("AUDIO")
                        put("TEXT")
                    }
                    put("responseModalities", modalities)
                    val speechConfig = JSONObject().apply {
                        val voiceConfig = JSONObject().apply {
                            val prebuiltVoiceConfig = JSONObject().apply {
                                put("voiceName", "Aoede")
                            }
                            put("prebuiltVoiceConfig", prebuiltVoiceConfig)
                        }
                        put("voiceConfig", voiceConfig)
                    }
                    put("speechConfig", speechConfig)
                }
                put("generationConfig", genConfig)

                val systemInstruction = JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().put("text", "You are Rai King, an enthusiastic, energetic AI Ludo grandmaster and match commentator. Talk directly to the player with fun commentary, strategy tips, and friendly banter."))
                    }
                    put("parts", parts)
                }
                put("systemInstruction", systemInstruction)
            }
            put("setup", setupObj)
        }

        ws.send(setupPayload.toString())
    }

    private fun handleServerMessage(jsonText: String) {
        try {
            val root = JSONObject(jsonText)
            val serverContent = root.optJSONObject("serverContent") ?: return
            val modelTurn = serverContent.optJSONObject("modelTurn")
            val parts = modelTurn?.optJSONArray("parts")

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)

                    // Text transcription chunk
                    val text = part.optString("text")
                    if (text.isNotBlank()) {
                        scope.launch(Dispatchers.Main) {
                            _voiceState.update { state ->
                                val updatedResponse = state.currentAiResponse + text
                                state.copy(
                                    currentAiResponse = updatedResponse,
                                    connectionState = LiveConnectionState.SPEAKING
                                )
                            }
                        }
                    }

                    // Audio chunk
                    val inlineData = part.optJSONObject("inlineData")
                    if (inlineData != null) {
                        val base64Audio = inlineData.optString("data")
                        if (base64Audio.isNotBlank()) {
                            val pcmBytes = Base64.decode(base64Audio, Base64.DEFAULT)
                            playPcmAudio(pcmBytes)
                        }
                    }
                }
            }

            if (serverContent.optBoolean("turnComplete", false)) {
                scope.launch(Dispatchers.Main) {
                    val response = _voiceState.value.currentAiResponse
                    if (response.isNotBlank()) {
                        _voiceState.update { state ->
                            val newMsg = LiveVoiceMessage(
                                sender = "Rai AI Grandmaster",
                                text = response,
                                isUser = false
                            )
                            state.copy(
                                currentAiResponse = "",
                                messageHistory = state.messageHistory + newMsg,
                                connectionState = LiveConnectionState.LISTENING
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiLiveVoice", "Error handling server message", e)
        }
    }

    fun sendTextMessage(userText: String) {
        if (userText.isBlank()) return

        val userMsg = LiveVoiceMessage(
            sender = "You",
            text = userText,
            isUser = true
        )

        _voiceState.update {
            it.copy(messageHistory = it.messageHistory + userMsg)
        }

        val ws = webSocket
        if (ws != null && _voiceState.value.connectionState == LiveConnectionState.CONNECTED) {
            try {
                val messagePayload = JSONObject().apply {
                    val clientContent = JSONObject().apply {
                        val turns = JSONArray().apply {
                            val turn = JSONObject().apply {
                                put("role", "user")
                                val parts = JSONArray().apply {
                                    put(JSONObject().put("text", userText))
                                }
                                put("parts", parts)
                            }
                            put(turn)
                        }
                        put("turns", turns)
                        put("turnComplete", true)
                    }
                    put("clientContent", clientContent)
                }
                ws.send(messagePayload.toString())
            } catch (e: Exception) {
                simulateAiResponse(userText)
            }
        } else {
            simulateAiResponse(userText)
        }
    }

    fun toggleMicMute() {
        _voiceState.update { it.copy(isMicMuted = !it.isMicMuted) }
    }

    fun triggerGameCommentary(eventDescription: String) {
        sendTextMessage("Match update: $eventDescription. Give quick commentary!")
    }

    private fun startMicrophoneStreaming() {
        recordingJob?.cancel()
        recordingJob = scope.launch(Dispatchers.IO) {
            // 1. Verify runtime permission
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                Log.i("GeminiLiveVoice", "RECORD_AUDIO permission not granted; microphone streaming disabled")
                return@launch
            }

            // 2. Verify microphone hardware feature exists
            val hasMicFeature = context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
            if (!hasMicFeature) {
                Log.i("GeminiLiveVoice", "Device has no microphone hardware; microphone streaming disabled")
                return@launch
            }

            // 3. Verify minBufferSize
            val minBufferSize = try {
                AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            } catch (t: Throwable) {
                -1
            }

            if (minBufferSize <= 0) {
                Log.w("GeminiLiveVoice", "AudioRecord invalid minBufferSize: $minBufferSize; microphone streaming disabled")
                return@launch
            }

            val bufferSize = minBufferSize.coerceAtLeast(2048)

            // 4. Safely initialize AudioRecord instance
            var record: AudioRecord? = null
            try {
                record = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
            } catch (t: Throwable) {
                Log.w("GeminiLiveVoice", "AudioRecord instantiation error: ${t.message}")
                record = null
            }

            if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
                Log.w("GeminiLiveVoice", "AudioRecord failed to initialize or uninitialized state; microphone streaming disabled")
                try { record?.release() } catch (t: Throwable) {}
                audioRecord = null
                return@launch
            }

            audioRecord = record

            try {
                record.startRecording()
                if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                    Log.w("GeminiLiveVoice", "AudioRecord failed to enter recording state")
                    return@launch
                }

                val audioBuffer = ByteArray(bufferSize)

                while (isActive && _voiceState.value.connectionState != LiveConnectionState.DISCONNECTED) {
                    if (!_voiceState.value.isMicMuted) {
                        val read = record.read(audioBuffer, 0, bufferSize)
                        if (read > 0) {
                            var sum = 0.0
                            for (i in 0 until read step 2) {
                                val sample = (audioBuffer[i].toInt() or (audioBuffer[i + 1].toInt() shl 8)).toShort()
                                sum += Math.abs(sample.toDouble())
                            }
                            val avg = (sum / (read / 2)).toFloat() / 32768f
                            _voiceState.update { it.copy(audioAmplitude = avg.coerceIn(0.05f, 1f)) }

                            val base64Data = Base64.encodeToString(audioBuffer, 0, read, Base64.NO_WRAP)
                            val realtimePayload = JSONObject().apply {
                                val realtimeInput = JSONObject().apply {
                                    val mediaChunks = JSONArray().apply {
                                        val chunk = JSONObject().apply {
                                            put("mimeType", "audio/pcm;rate=16000")
                                            put("data", base64Data)
                                        }
                                        put(chunk)
                                    }
                                    put("mediaChunks", mediaChunks)
                                }
                                put("realtimeInput", realtimeInput)
                            }
                            webSocket?.send(realtimePayload.toString())
                        }
                    } else {
                        _voiceState.update { it.copy(audioAmplitude = 0.05f) }
                        delay(100L)
                    }
                }
            } catch (t: Throwable) {
                Log.w("GeminiLiveVoice", "Microphone streaming error: ${t.message}")
            } finally {
                try {
                    if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        record.stop()
                    }
                } catch (t: Throwable) {}
                try {
                    record.release()
                } catch (t: Throwable) {}
                if (audioRecord == record) {
                    audioRecord = null
                }
            }
        }
    }

    private fun stopMicrophoneStreaming() {
        recordingJob?.cancel()
        recordingJob = null
        try {
            val record = audioRecord
            if (record != null) {
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    record.stop()
                }
                record.release()
            }
        } catch (e: Throwable) { }
        audioRecord = null
    }

    private fun playPcmAudio(pcmBytes: ByteArray) {
        scope.launch(Dispatchers.IO) {
            try {
                val bufferSize = AudioTrack.getMinBufferSize(
                    24000,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(pcmBytes.size)

                if (bufferSize <= 0) return@launch

                if (audioTrack == null) {
                    val track = try {
                        AudioTrack.Builder()
                            .setAudioFormat(
                                AudioFormat.Builder()
                                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                    .setSampleRate(24000)
                                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                    .build()
                            )
                            .setBufferSizeInBytes(bufferSize)
                            .build()
                    } catch (t: Throwable) {
                        null
                    }

                    if (track?.state == AudioTrack.STATE_INITIALIZED) {
                        audioTrack = track
                        track.play()
                    } else {
                        try { track?.release() } catch (t: Throwable) {}
                    }
                }

                if (audioTrack?.state == AudioTrack.STATE_INITIALIZED) {
                    audioTrack?.write(pcmBytes, 0, pcmBytes.size)
                }
            } catch (e: Throwable) {
                Log.w("GeminiLiveVoice", "Error playing PCM audio: ${e.message}")
            }
        }
    }

    private fun startSimulatedLiveSession(greeting: String) {
        _voiceState.update {
            it.copy(
                connectionState = LiveConnectionState.CONNECTED,
                statusMessage = greeting,
                messageHistory = listOf(
                    LiveVoiceMessage(
                        sender = "Rai AI Grandmaster",
                        text = "Greetings Champion! I am your Live AI Grandmaster. Ask me for strategy, roll predictions, or commentary while you play!",
                        isUser = false
                    )
                )
            )
        }

        // Simulate lively audio amplitude pulses for engaging UI
        scope.launch {
            while (isActive && _voiceState.value.connectionState == LiveConnectionState.CONNECTED) {
                delay(300L)
                val simulatedAmp = (Math.sin(System.currentTimeMillis() / 400.0).toFloat() * 0.3f + 0.4f).coerceIn(0.1f, 0.9f)
                _voiceState.update { it.copy(audioAmplitude = simulatedAmp) }
            }
        }
    }

    private fun simulateAiResponse(query: String) {
        scope.launch {
            _voiceState.update { it.copy(connectionState = LiveConnectionState.SPEAKING) }
            delay(600L)

            val lower = query.lowercase()
            val reply = when {
                "six" in lower || "roll" in lower ->
                    "Roll with confidence! In Ludo, high tempo dice shaking creates maximum momentum. Keep your tokens mobile!"
                "move" in lower || "strategy" in lower ->
                    "Golden rule: Never leave an isolated token 5 to 7 spaces in front of an aggressive opponent! Secure the star safe cell."
                "bot" in lower || "trash" in lower ->
                    "Haha! Tell those bots Suraj Rai didn't build them to stand a chance against your rolling skills!"
                "win" in lower || "champion" in lower ->
                    "You've got the Emperor mindset! Focus on getting at least 2 tokens home first to force your opponent on defense."
                else ->
                    "I hear you loud and clear! Play bold, protect your home column runway, and let the 6s roll!"
            }

            _voiceState.update { state ->
                state.copy(
                    connectionState = LiveConnectionState.CONNECTED,
                    messageHistory = state.messageHistory + LiveVoiceMessage(
                        sender = "Rai AI Grandmaster",
                        text = reply,
                        isUser = false
                    )
                )
            }
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        stopMicrophoneStreaming()
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        _voiceState.update {
            it.copy(
                connectionState = LiveConnectionState.DISCONNECTED,
                statusMessage = "Session ended"
            )
        }
    }

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }
}
