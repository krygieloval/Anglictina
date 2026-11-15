package com.example.ui.child

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.VocabWord
import com.example.ui.components.SpeakButton
import com.example.ui.components.WordIllustrationBox
import com.example.ui.theme.MintContainer
import com.example.ui.theme.MintDarkText
import com.example.ui.theme.MintTertiary
import com.example.util.EnglishSpeechListener
import com.example.util.PronunciationEvaluator

@Composable
fun PronunciationExerciseScreen(
    words: List<VocabWord>,
    fallbackEmoji: String,
    onSpeakWord: (String) -> Unit,
    onBack: () -> Unit,
    onFinish: (wordOutcomes: Map<VocabWord, Boolean>, starsEarned: Int, score: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val queue = remember(words) { words.shuffled().take(10) }
    var currentIndex by remember { mutableIntStateOf(0) }
    val currentWord = queue.getOrNull(currentIndex) ?: return

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isListening by remember { mutableStateOf(false) }
    var rmsLevel by remember { mutableFloatStateOf(0f) }
    var evaluation by remember(currentIndex) {
        mutableStateOf<PronunciationEvaluator.EvaluationResult?>(null)
    }
    var speechError by remember(currentIndex) { mutableStateOf<String?>(null) }
    val outcomes = remember { mutableStateMapOf<VocabWord, Boolean>() }

    val speechListener = remember(currentIndex) {
        EnglishSpeechListener(
            context = context,
            onRmsChanged = { rms -> rmsLevel = ((rms + 2f) / 12f).coerceIn(0f, 1f) },
            onResults = { candidates ->
                isListening = false
                rmsLevel = 0f
                val eval = PronunciationEvaluator.evaluate(currentWord.englishWord, candidates)
                evaluation = eval
                if (eval.isAccepted) {
                    outcomes[currentWord] = true
                } else if (!outcomes.containsKey(currentWord)) {
                    outcomes[currentWord] = false
                }
            },
            onError = { msg ->
                isListening = false
                rmsLevel = 0f
                speechError = msg
            }
        )
    }

    DisposableEffect(currentIndex) {
        onDispose { speechListener.stopListening() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            speechError = null
            isListening = true
            speechListener.startListening()
        } else {
            speechError = "Pro kontrolu výslovnosti je potřeba povolit přístup k mikrofonu."
        }
    }

    val pulseScale by animateFloatAsState(
        targetValue = if (isListening) 1f + (rmsLevel * 0.25f) else 1f,
        animationSpec = spring(),
        label = "micPulse"
    )

    fun advanceNext() {
        speechListener.stopListening()
        isListening = false
        if (!outcomes.containsKey(currentWord)) {
            outcomes[currentWord] = false
        }
        if (currentIndex + 1 < queue.size) {
            currentIndex += 1
        } else {
            val correct = outcomes.values.count { it }
            val total = queue.size.coerceAtLeast(1)
            val pct = (correct * 100) / total
            val stars = when {
                pct >= 80 -> 3
                pct >= 50 -> 2
                else -> 1
            }
            onFinish(outcomes.toMap(), stars, correct * 30)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 760.dp)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("pronunciation_back_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Výslovnost", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Slovíčko ${currentIndex + 1} z ${queue.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LinearProgressIndicator(
            progress = { (currentIndex + 1).toFloat() / queue.size.coerceAtLeast(1).toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
        )

        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WordIllustrationBox(
                    cacheKey = currentWord.id,
                    imageBase64 = currentWord.imageBase64,
                    fallbackEmoji = fallbackEmoji,
                    englishWord = currentWord.englishWord,
                    contentDescription = currentWord.czechTranslation,
                    modifier = Modifier.size(180.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = currentWord.englishWord,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                SpeakButton(
                    onClick = { onSpeakWord(currentWord.englishWord) },
                    label = "Poslechnout si vzor"
                )
            }
        }

        // Microphone button with animated pulse ring
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            if (isListening) {
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                )
            }
            Button(
                onClick = {
                    if (isListening) {
                        speechListener.stopListening()
                        isListening = false
                    } else if (!hasMicPermission) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        speechError = null
                        isListening = true
                        speechListener.startListening()
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isListening) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                ),
                modifier = Modifier
                    .size(88.dp)
                    .testTag("pronunciation_mic_button")
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                    contentDescription = if (isListening) "Zastavit nahrávání" else "Říct slovíčko",
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        Text(
            text = when {
                isListening -> "Poslouchám... Řekni anglicky \"${currentWord.englishWord}\""
                !speechListener.isAvailable() -> "Rozpoznávání řeči není na tomto zařízení dostupné (můžeš si poslechnout výslovnost a pokračovat)."
                else -> "Ťukni na mikrofon a vyslov slovíčko nahlas!"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (speechError != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = speechError!!,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        evaluation?.let { eval ->
            Surface(
                color = if (eval.isAccepted) MintContainer else MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (eval.isAccepted) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MintTertiary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = if (eval.isAccepted) {
                                "Skvělá výslovnost! (${eval.matchPercent} %)"
                            } else {
                                "Skoro to bylo! (${eval.matchPercent} %) Zkus to znovu."
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (eval.isAccepted) MintDarkText else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    if (eval.heardText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Slyšel jsem: \"${eval.heardText}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (eval.isAccepted) MintDarkText else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (evaluation?.isAccepted == true) {
                Button(
                    onClick = { advanceNext() },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("pronunciation_next_button")
                ) {
                    Text(
                        if (currentIndex + 1 < queue.size) "Další slovíčko" else "Dokončit cvičení",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                }
            } else {
                OutlinedButton(
                    onClick = { advanceNext() },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("pronunciation_skip_button")
                ) {
                    Text(
                        if (currentIndex + 1 < queue.size) "Přeskočit na další" else "Dokončit"
                    )
                }
            }
        }
    }
}
