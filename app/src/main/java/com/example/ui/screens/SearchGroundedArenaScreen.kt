package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ai.GeminiSearchService
import com.example.ai.GroundedTriviaQuestion
import com.example.ai.SearchGuruResponse
import com.example.audio.SoundManager
import com.example.data.model.UserProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

data class KbcLadderStep(
    val levelNumber: Int,
    val prizeAmount: Long,
    val label: String,
    val isPadav: Boolean = false
)

val SANATAN_KBC_LADDER = listOf(
    KbcLadderStep(1, 1_000L, "🪙 1,000"),
    KbcLadderStep(2, 2_000L, "🪙 2,000"),
    KbcLadderStep(3, 3_000L, "🪙 3,000"),
    KbcLadderStep(4, 5_000L, "🪙 5,000"),
    KbcLadderStep(5, 10_000L, "🪙 10,000", isPadav = true), // पड़ाव १
    KbcLadderStep(6, 20_000L, "🪙 20,000"),
    KbcLadderStep(7, 40_000L, "🪙 40,000"),
    KbcLadderStep(8, 80_000L, "🪙 80,000"),
    KbcLadderStep(9, 160_000L, "🪙 1,60,000"),
    KbcLadderStep(10, 320_000L, "🪙 3,20,000", isPadav = true), // पड़ाव २
    KbcLadderStep(11, 640_000L, "🪙 6,40,000"),
    KbcLadderStep(12, 1_250_000L, "🪙 12,50,000"),
    KbcLadderStep(13, 2_500_000L, "🪙 25,00,000"),
    KbcLadderStep(14, 5_000_000L, "🪙 50,00,000"),
    KbcLadderStep(15, 10_000_000L, "🪙 1 Crore (महाविजेता)", isPadav = true),
    KbcLadderStep(16, 70_000_000L, "🪙 7 Crore (सनातन शिरोमणि)", isPadav = true)
)

@Composable
fun SearchGroundedArenaScreen(
    userProfile: UserProfile,
    onAwardCoins: (Long) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    val searchService = remember { GeminiSearchService() }
    val scope = rememberCoroutineScope()

    // Game Progression States
    var currentLevelIndex by remember { mutableIntStateOf(0) }
    val currentLevel = SANATAN_KBC_LADDER[currentLevelIndex]

    var currentTrivia by remember { mutableStateOf<GroundedTriviaQuestion?>(null) }
    var isLoadingTrivia by remember { mutableStateOf(false) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var isLocked by remember { mutableStateOf(false) }
    var isLockingSuspense by remember { mutableStateOf(false) }
    var isAnswerRevealed by remember { mutableStateOf(false) }
    var isAnswerCorrect by remember { mutableStateOf(false) }

    // Coins & Padav tracking
    var guaranteedSafeCoins by remember { mutableStateOf(0L) }
    var currentWonSessionCoins by remember { mutableStateOf(0L) }

    // Lifeline States
    var isFiftyFiftyUsed by remember { mutableStateOf(false) }
    var isAudiencePollUsed by remember { mutableStateOf(false) }
    var isAskGuruUsed by remember { mutableStateOf(false) }
    var isFlipQuestionUsed by remember { mutableStateOf(false) }

    var hiddenOptionIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showAudiencePollDialog by remember { mutableStateOf(false) }
    var audiencePollPercentages by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var showAskGuruDialog by remember { mutableStateOf(false) }
    var guruResponse by remember { mutableStateOf<SearchGuruResponse?>(null) }
    var isLoadingGuru by remember { mutableStateOf(false) }

    // Dialogs & Navigation
    var showLadderDialog by remember { mutableStateOf(false) }
    var showQuitConfirmDialog by remember { mutableStateOf(false) }
    var showGameOverDialog by remember { mutableStateOf(false) }
    var gameOverTitle by remember { mutableStateOf("") }
    var gameOverMessage by remember { mutableStateOf("") }

    // Non-repetition history
    val askedQuestionsHistory = remember { mutableStateListOf<String>() }

    // 45s Tik-Tiki Countdown Timer
    var timeLeft by remember { mutableIntStateOf(45) }
    var isTimerActive by remember { mutableStateOf(false) }

    // Custom Sanatan Query section state
    var customGuruQuery by remember { mutableStateOf("") }
    var customGuruAnswer by remember { mutableStateOf<SearchGuruResponse?>(null) }
    var isCustomGuruSearching by remember { mutableStateOf(false) }

    // Function to load a question
    fun loadQuestion(levelIdx: Int) {
        scope.launch {
            isLoadingTrivia = true
            selectedOptionIndex = null
            isLocked = false
            isLockingSuspense = false
            isAnswerRevealed = false
            isAnswerCorrect = false
            hiddenOptionIndices = emptySet()
            timeLeft = 45
            isTimerActive = false

            val prize = SANATAN_KBC_LADDER[levelIdx].prizeAmount
            val question = searchService.fetchSearchGroundedTrivia(
                previousQuestions = askedQuestionsHistory.toList(),
                level = levelIdx + 1,
                prizeCoins = prize
            )
            currentTrivia = question
            askedQuestionsHistory.add(question.question)
            isLoadingTrivia = false
            isTimerActive = true
        }
    }

    // Timer effect
    LaunchedEffect(currentTrivia, isTimerActive, isLocked) {
        if (isTimerActive && !isLocked && currentTrivia != null) {
            while (timeLeft > 0 && isTimerActive && !isLocked) {
                delay(1000)
                timeLeft -= 1
            }
            if (timeLeft == 0 && !isLocked) {
                // Time Expired
                soundManager.playKbcWrong()
                isLocked = true
                isAnswerRevealed = true
                isAnswerCorrect = false
                gameOverTitle = "⏰ समय समाप्त! (Time Out)"
                gameOverMessage = "आपका समय समाप्त हो गया। आप सुरक्षित पड़ाव की राशि 🪙 %,d जीतते हैं!".format(guaranteedSafeCoins)
                if (guaranteedSafeCoins > 0) {
                    onAwardCoins(guaranteedSafeCoins)
                }
                showGameOverDialog = true
            }
        }
    }

    // Initial Load
    LaunchedEffect(Unit) {
        loadQuestion(0)
    }

    // Lifeline 1: 50:50
    fun useFiftyFifty() {
        if (isFiftyFiftyUsed || isLocked || currentTrivia == null) return
        soundManager.playKbcLifeline()
        val correctIdx = currentTrivia!!.correctIndex
        val wrongIndices = (0..3).filter { it != correctIdx }.shuffled()
        // Hide two wrong indices
        hiddenOptionIndices = wrongIndices.take(2).toSet()
        isFiftyFiftyUsed = true
    }

    // Lifeline 2: Audience Poll
    fun useAudiencePoll() {
        if (isAudiencePollUsed || isLocked || currentTrivia == null) return
        soundManager.playKbcLifeline()
        val correctIdx = currentTrivia!!.correctIndex

        // Generate high percentage for correct option (~68-84%)
        val correctPct = Random.nextInt(68, 85)
        val remaining = 100 - correctPct
        val remainingIndices = (0..3).filter { it != correctIdx && it !in hiddenOptionIndices }

        val distribution = mutableMapOf<Int, Int>()
        distribution[correctIdx] = correctPct

        if (remainingIndices.isNotEmpty()) {
            var pool = remaining
            remainingIndices.forEachIndexed { idx, optIdx ->
                if (idx == remainingIndices.lastIndex) {
                    distribution[optIdx] = pool
                } else {
                    val share = Random.nextInt(2, (pool / 2).coerceAtLeast(4))
                    distribution[optIdx] = share
                    pool -= share
                }
            }
        }
        // Fill zero for hidden options
        hiddenOptionIndices.forEach { distribution[it] = 0 }

        audiencePollPercentages = distribution
        isAudiencePollUsed = true
        showAudiencePollDialog = true
    }

    // Lifeline 3: Ask Search Guru
    fun useAskGuru() {
        if (isAskGuruUsed || isLocked || currentTrivia == null) return
        soundManager.playKbcLifeline()
        isAskGuruUsed = true
        showAskGuruDialog = true
        scope.launch {
            isLoadingGuru = true
            val promptQuery = "In Sanatan Dharma scripture, for this question: '${currentTrivia!!.question}', which is the authentic answer and scriptural citation?"
            guruResponse = searchService.askSearchGroundedGuru(promptQuery)
            isLoadingGuru = false
        }
    }

    // Lifeline 4: Flip Question
    fun useFlipQuestion() {
        if (isFlipQuestionUsed || isLocked) return
        soundManager.playKbcLifeline()
        isFlipQuestionUsed = true
        loadQuestion(currentLevelIndex)
    }

    // Restart game
    fun restartGame() {
        currentLevelIndex = 0
        guaranteedSafeCoins = 0L
        currentWonSessionCoins = 0L
        isFiftyFiftyUsed = false
        isAudiencePollUsed = false
        isAskGuruUsed = false
        isFlipQuestionUsed = false
        showGameOverDialog = false
        loadQuestion(0)
    }

    // Pulse animation for locking suspense
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF071330), // Deep Vedic Royal Navy
                        Color(0xFF0C1E4A),
                        Color(0xFF050B1B)
                    )
                )
            )
            .testTag("sanatan_kbc_arena_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Top Navigation & Balance Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("kbc_back_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "🕉️", fontSize = 18.sp)
                        Text(
                            text = "सनातन ज्ञान महाकुंभ",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFBBF24) // Gold
                        )
                    }
                    Text(
                        text = "KBC Style • Grounded with Google Search",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Balance Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.2f))
                        .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🪙 %,d".format(userProfile.coins),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFBBF24)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Current Level & Safe Haven Header Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF132247)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "प्रश्न ${currentLevel.levelNumber} / 16",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.SemiBold
                            )
                            if (currentLevel.isPadav) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.25f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("🛡️ पड़ाव", fontSize = 10.sp, color = Color(0xFF34D399), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text(
                            text = currentLevel.label,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFBBF24)
                        )
                    }

                    // Timer & Ladder Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Timer Indicator
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (timeLeft > 15) Color(0xFF10B981).copy(alpha = 0.2f)
                                    else if (timeLeft > 7) Color(0xFFF59E0B).copy(alpha = 0.25f)
                                    else Color(0xFFEF4444).copy(alpha = 0.3f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (timeLeft > 15) Color(0xFF10B981) else if (timeLeft > 7) Color(0xFFF59E0B) else Color(0xFFEF4444),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "${timeLeft}s",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        // View Ladder Button
                        IconButton(
                            onClick = {
                                soundManager.playButtonClick()
                                showLadderDialog = true
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E3A8A))
                                .testTag("btn_kbc_ladder")
                        ) {
                            Icon(Icons.Default.FormatListNumbered, contentDescription = "Ladder", tint = Color(0xFF93C5FD), modifier = Modifier.size(18.dp))
                        }

                        // Quit Game Button
                        IconButton(
                            onClick = {
                                soundManager.playButtonClick()
                                showQuitConfirmDialog = true
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF7F1D1D).copy(alpha = 0.7f))
                                .testTag("btn_kbc_quit")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Quit", tint = Color(0xFFFCA5A5), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4 KBC Lifelines Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Lifeline 1: 50:50
                LifelineChip(
                    title = "50:50",
                    icon = "✂️",
                    isUsed = isFiftyFiftyUsed,
                    enabled = !isFiftyFiftyUsed && !isLocked && !isLoadingTrivia,
                    onClick = { useFiftyFifty() },
                    modifier = Modifier.weight(1f).testTag("lifeline_50_50")
                )

                // Lifeline 2: Audience Poll
                LifelineChip(
                    title = "दर्शक मत",
                    icon = "📊",
                    isUsed = isAudiencePollUsed,
                    enabled = !isAudiencePollUsed && !isLocked && !isLoadingTrivia,
                    onClick = { useAudiencePoll() },
                    modifier = Modifier.weight(1f).testTag("lifeline_audience_poll")
                )

                // Lifeline 3: Ask Search Guru
                LifelineChip(
                    title = "सर्च गुरु",
                    icon = "🪷",
                    isUsed = isAskGuruUsed,
                    enabled = !isAskGuruUsed && !isLocked && !isLoadingTrivia,
                    onClick = { useAskGuru() },
                    modifier = Modifier.weight(1f).testTag("lifeline_ask_guru")
                )

                // Lifeline 4: Flip Question
                LifelineChip(
                    title = "बदलो",
                    icon = "🔄",
                    isUsed = isFlipQuestionUsed,
                    enabled = !isFlipQuestionUsed && !isLocked && !isLoadingTrivia,
                    onClick = { useFlipQuestion() },
                    modifier = Modifier.weight(1f).testTag("lifeline_flip_question")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Trivia Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Question Card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E3D)),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFD97706)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFD97706).copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "📖 सनातन शास्त्र प्रश्न • कठिन स्तर (Hard)",
                                        fontSize = 11.sp,
                                        color = Color(0xFFFBBF24),
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (guaranteedSafeCoins > 0) {
                                    Text(
                                        text = "सुरक्षित: 🪙 %,d".format(guaranteedSafeCoins),
                                        fontSize = 11.sp,
                                        color = Color(0xFF34D399),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (isLoadingTrivia) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        CircularProgressIndicator(color = Color(0xFFF59E0B), strokeWidth = 3.dp)
                                        Text(
                                            text = "गूगल सर्च द्वारा नवीन सनातन प्रश्न तैयार किया जा रहा है...",
                                            fontSize = 12.sp,
                                            color = Color(0xFF94A3B8),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                currentTrivia?.let { trivia ->
                                    Text(
                                        text = trivia.question,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        lineHeight = 22.sp
                                    )

                                    // Options List
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        trivia.options.forEachIndexed { optIndex, optionText ->
                                            val isHidden = optIndex in hiddenOptionIndices
                                            val isSelected = selectedOptionIndex == optIndex
                                            val isCorrect = optIndex == trivia.correctIndex

                                            val optionLetter = ('A' + optIndex)

                                            if (!isHidden) {
                                                // Option background and border color determination
                                                val (bgColor, borderColor) = when {
                                                    isAnswerRevealed && isCorrect -> Color(0xFF065F46) to Color(0xFF10B981) // Correct Green
                                                    isAnswerRevealed && isSelected && !isCorrect -> Color(0xFF7F1D1D) to Color(0xFFEF4444) // Wrong Red
                                                    isLockingSuspense && isSelected -> Color(0xFFB45309).copy(alpha = pulseAlpha) to Color(0xFFFBBF24) // Blinking Lock
                                                    isSelected -> Color(0xFF78350F) to Color(0xFFF59E0B) // Selected Amber
                                                    else -> Color(0xFF14244D) to Color(0xFF2E447B) // Default Idle
                                                }

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(bgColor)
                                                        .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                                                        .clickable(enabled = !isLocked && !isLockingSuspense) {
                                                            soundManager.playButtonClick()
                                                            selectedOptionIndex = optIndex
                                                        }
                                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                                        .testTag("kbc_option_$optIndex"),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .clip(CircleShape)
                                                                .background(
                                                                    if (isSelected || (isAnswerRevealed && isCorrect)) Color(0xFFF59E0B)
                                                                    else Color(0xFF1E3A8A)
                                                                ),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = optionLetter.toString(),
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Black,
                                                                color = if (isSelected || (isAnswerRevealed && isCorrect)) Color(0xFF0F172A) else Color.White
                                                            )
                                                        }

                                                        Spacer(modifier = Modifier.width(10.dp))

                                                        Text(
                                                            text = optionText,
                                                            fontSize = 13.sp,
                                                            color = Color.White,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }

                                                    // Status Icon
                                                    if (isAnswerRevealed) {
                                                        if (isCorrect) {
                                                            Icon(Icons.Default.CheckCircle, contentDescription = "Correct", tint = Color(0xFF34D399), modifier = Modifier.size(20.dp))
                                                        } else if (isSelected) {
                                                            Icon(Icons.Default.Close, contentDescription = "Wrong", tint = Color(0xFFF87171), modifier = Modifier.size(20.dp))
                                                        }
                                                    } else if (isSelected) {
                                                        Icon(Icons.Default.Lock, contentDescription = "Selected", tint = Color(0xFFFBBF24), modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            } else {
                                                // 50:50 Disabled/Hidden placeholder
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(44.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(Color(0xFF0B142B).copy(alpha = 0.5f))
                                                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp)),
                                                    contentAlignment = Alignment.CenterStart
                                                ) {
                                                    Text(
                                                        text = "   $optionLetter. — [50:50 हटाया गया]",
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF475569),
                                                        fontWeight = FontWeight.Normal
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Lock Answer Button ("कंप्यूटर जी, लॉक किया जाए!")
                item {
                    if (selectedOptionIndex != null && !isLocked && !isLoadingTrivia) {
                        Button(
                            onClick = {
                                if (!isLockingSuspense) {
                                    scope.launch {
                                        isLockingSuspense = true
                                        isTimerActive = false
                                        soundManager.playKbcLock()
                                        delay(1300) // Suspense delay!
                                        isLockingSuspense = false
                                        isLocked = true
                                        isAnswerRevealed = true

                                        val isCorrect = selectedOptionIndex == currentTrivia!!.correctIndex
                                        isAnswerCorrect = isCorrect

                                        if (isCorrect) {
                                            soundManager.playKbcCorrect()
                                            currentWonSessionCoins = currentLevel.prizeAmount
                                            if (currentLevel.isPadav) {
                                                guaranteedSafeCoins = currentLevel.prizeAmount
                                            }
                                        } else {
                                            soundManager.playKbcWrong()
                                            if (guaranteedSafeCoins > 0) {
                                                onAwardCoins(guaranteedSafeCoins)
                                            }
                                            gameOverTitle = "❌ गलत उत्तर! (Game Over)"
                                            gameOverMessage = "सही उत्तर था: '${currentTrivia!!.options[currentTrivia!!.correctIndex]}'.\nआप सुरक्षित पड़ाव की राशि 🪙 %,d जीतते हैं!".format(guaranteedSafeCoins)
                                            showGameOverDialog = true
                                        }
                                    }
                                }
                            },
                            enabled = !isLockingSuspense,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_lock_answer")
                        ) {
                            if (isLockingSuspense) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("कंप्यूटर जी, ताला लगाया जा रहा है...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Lock, contentDescription = "Lock", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                val letter = ('A' + selectedOptionIndex!!)
                                Text(
                                    text = "कंप्यूटर जी, विकल्प ($letter) लॉक किया जाए!",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                // Explanation & Next Level Action (Post Answer Reveal)
                item {
                    if (isAnswerRevealed && currentTrivia != null) {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isAnswerCorrect) Color(0xFF07271E) else Color(0xFF2D1016)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isAnswerCorrect) Color(0xFF10B981) else Color(0xFFEF4444)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(text = if (isAnswerCorrect) "🎉" else "📜", fontSize = 18.sp)
                                    Text(
                                        text = if (isAnswerCorrect) "अद्भुत! सही उत्तर (+${currentLevel.label})" else "शास्त्र संदर्भ व व्याख्या",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAnswerCorrect) Color(0xFF34D399) else Color(0xFFF87171)
                                    )
                                }

                                Text(
                                    text = currentTrivia!!.explanation,
                                    fontSize = 12.sp,
                                    color = Color(0xFFE2E8F0),
                                    lineHeight = 18.sp
                                )

                                Text(
                                    text = "🌐 प्रमाण: ${currentTrivia!!.searchSourceCitation}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF38BDF8)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                if (isAnswerCorrect) {
                                    if (currentLevelIndex < SANATAN_KBC_LADDER.lastIndex) {
                                        val nextStep = SANATAN_KBC_LADDER[currentLevelIndex + 1]
                                        Button(
                                            onClick = {
                                                soundManager.playButtonClick()
                                                currentLevelIndex += 1
                                                loadQuestion(currentLevelIndex)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("btn_next_kbc_question")
                                        ) {
                                            Text(
                                                text = "अगला प्रश्न (Level ${nextStep.levelNumber}: ${nextStep.label}) →",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        // 7 Crore Champion Won!
                                        Button(
                                            onClick = {
                                                soundManager.playVictory()
                                                onAwardCoins(currentLevel.prizeAmount)
                                                gameOverTitle = "👑 सनातन ज्ञान शिरोमणि महाविजेता!"
                                                gameOverMessage = "अद्भुत! आपने सभी 16 कठिन स्तर पार करके 🪙 7 Crore Coins जीत लिए हैं!"
                                                showGameOverDialog = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("🏆 7 Crore Coins अपने खाते में जोड़ें!", fontSize = 14.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            soundManager.playButtonClick()
                                            restartGame()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("btn_restart_game")
                                    ) {
                                        Text("नया खेल शुरू करें (Start New Game)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2: Custom Sanatan Dharma Search Guru Inquiry
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1A38)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                                Text(
                                    text = "सनातन सर्च गुरु (Ask Any Scripture Question)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = customGuruQuery,
                                    onValueChange = { customGuruQuery = it },
                                    placeholder = { Text("रामायण, महाभारत, उपनिषद या कोई भी शास्त्र पूछें...", fontSize = 11.sp, color = Color(0xFF64748B)) },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFFF59E0B),
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedContainerColor = Color(0xFF0A132B),
                                        unfocusedContainerColor = Color(0xFF0A132B)
                                    ),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                IconButton(
                                    onClick = {
                                        if (customGuruQuery.isNotBlank() && !isCustomGuruSearching) {
                                            scope.launch {
                                                isCustomGuruSearching = true
                                                customGuruAnswer = searchService.askSearchGroundedGuru(customGuruQuery)
                                                isCustomGuruSearching = false
                                            }
                                        }
                                    },
                                    enabled = customGuruQuery.isNotBlank() && !isCustomGuruSearching,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD97706))
                                ) {
                                    if (isCustomGuruSearching) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Ask", tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            // Quick query suggestions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "गीता २.४७ का अर्थ",
                                    "पाशुपतास्त्र रहस्य",
                                    "नासदीय सूक्त"
                                ).forEach { chip ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF1E2E59))
                                            .clickable {
                                                customGuruQuery = chip
                                                scope.launch {
                                                    isCustomGuruSearching = true
                                                    customGuruAnswer = searchService.askSearchGroundedGuru(chip)
                                                    isCustomGuruSearching = false
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(chip, fontSize = 10.sp, color = Color(0xFF93C5FD))
                                    }
                                }
                            }

                            customGuruAnswer?.let { answer ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF0B142B))
                                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .padding(10.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "🪷 गुरु उत्तर:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF34D399)
                                        )
                                        Text(
                                            text = answer.answer,
                                            fontSize = 11.sp,
                                            color = Color(0xFFE2E8F0),
                                            lineHeight = 16.sp
                                        )
                                        if (answer.citations.isNotEmpty()) {
                                            Text(
                                                text = "🌐 ${answer.citations.first()}",
                                                fontSize = 9.sp,
                                                color = Color(0xFF38BDF8)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- DIALOG 1: Audience Poll Dialog ---
        if (showAudiencePollDialog) {
            Dialog(onDismissRequest = { showAudiencePollDialog = false }) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E3D)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("📊", fontSize = 22.sp)
                            Text("दर्शक मत (Audience Poll)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                        }

                        Text(
                            text = "विद्वत सभा व दर्शकों का मत परिणाम:",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            currentTrivia?.options?.forEachIndexed { idx, optionText ->
                                val pct = audiencePollPercentages[idx] ?: 0
                                val letter = ('A' + idx)
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "$letter. $optionText",
                                            fontSize = 11.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "$pct%",
                                            fontSize = 12.sp,
                                            color = Color(0xFFFBBF24),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    LinearProgressIndicator(
                                        progress = { pct / 100f },
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                        color = Color(0xFFF59E0B),
                                        trackColor = Color(0xFF1E293B)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { showAudiencePollDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("धन्यवाद! प्रश्न पर लौटें", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- DIALOG 2: Ask Search Guru Dialog ---
        if (showAskGuruDialog) {
            Dialog(onDismissRequest = { showAskGuruDialog = false }) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1736)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("🪷", fontSize = 22.sp)
                            Text("सर्च गुरु (Phone-a-Guru)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                        }

                        if (isLoadingGuru) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(color = Color(0xFF38BDF8), strokeWidth = 3.dp)
                                    Text("गूगल सर्च व शास्त्रों द्वारा गुरुजी परामर्श ले रहे हैं...", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                }
                            }
                        } else {
                            guruResponse?.let { resp ->
                                Text(
                                    text = resp.answer,
                                    fontSize = 12.sp,
                                    color = Color(0xFFE2E8F0),
                                    lineHeight = 18.sp
                                )

                                if (resp.citations.isNotEmpty()) {
                                    Text(
                                        text = "🌐 शास्त्र प्रमाण: ${resp.citations.first()}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF7DD3FC),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { showAskGuruDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("समझ गया, धन्यवाद", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // --- DIALOG 3: 16-Step Prize Ladder Dialog ---
        if (showLadderDialog) {
            Dialog(onDismissRequest = { showLadderDialog = false }) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF09142E)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🪜 ज्ञान सीढ़ी (Prize Ladder)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                            IconButton(onClick = { showLadderDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.height(360.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            itemsIndexed(SANATAN_KBC_LADDER.reversed()) { _, step ->
                                val actualIdx = step.levelNumber - 1
                                val isCurrent = actualIdx == currentLevelIndex
                                val isPassed = actualIdx < currentLevelIndex

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isCurrent) Color(0xFFD97706)
                                            else if (isPassed) Color(0xFF065F46)
                                            else Color(0xFF132247)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "${step.levelNumber}.",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent) Color.White else Color(0xFF94A3B8)
                                        )
                                        if (step.isPadav) {
                                            Text("🛡️ पड़ाव", fontSize = 10.sp, color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Text(
                                        text = step.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isCurrent || step.isPadav) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isCurrent) Color.White else if (step.isPadav) Color(0xFFFBBF24) else Color(0xFFE2E8F0)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- DIALOG 4: Quit / Cash Out Confirmation Dialog ---
        if (showQuitConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showQuitConfirmDialog = false },
                title = { Text("खेल छोड़ें (Cash Out)?", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "क्या आप खेल छोड़कर अपनी संचित राशि 🪙 %,d Coins लेकर जाना चाहते हैं?".format(currentWonSessionCoins),
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showQuitConfirmDialog = false
                            if (currentWonSessionCoins > 0) {
                                onAwardCoins(currentWonSessionCoins)
                            }
                            gameOverTitle = "🛑 खेल समाप्त (Cashe Out)"
                            gameOverMessage = "आपने खेल छोड़ा और 🪙 %,d Coins अपने खाते में सुरक्षित किए!".format(currentWonSessionCoins)
                            showGameOverDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                    ) {
                        Text("हाँ, Coins लें")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showQuitConfirmDialog = false }) {
                        Text("खेल जारी रखें", color = Color(0xFF94A3B8))
                    }
                },
                containerColor = Color(0xFF0F1E3D)
            )
        }

        // --- DIALOG 5: Game Over / Victory Summary Dialog ---
        if (showGameOverDialog) {
            Dialog(onDismissRequest = { }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A142B)),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("🕉️", fontSize = 36.sp)
                        Text(
                            text = gameOverTitle,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFBBF24),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = gameOverMessage,
                            fontSize = 13.sp,
                            color = Color(0xFFE2E8F0),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onBack,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text("मुख्य पृष्ठ")
                            }

                            Button(
                                onClick = { restartGame() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
                            ) {
                                Text("पुनः खेलें", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LifelineChip(
    title: String,
    icon: String,
    isUsed: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isUsed) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFF132247)
    val borderColor = if (isUsed) Color(0xFF334155) else Color(0xFFF59E0B).copy(alpha = 0.7f)
    val textColor = if (isUsed) Color(0xFF64748B) else Color(0xFFFDE68A)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = icon, fontSize = 14.sp)
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            if (isUsed) {
                Text(
                    text = "प्रयुक्त",
                    fontSize = 8.sp,
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
