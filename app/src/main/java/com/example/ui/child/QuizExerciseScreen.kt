package com.example.ui.child

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.VocabWord
import com.example.ui.components.WordIllustrationBox
import com.example.ui.theme.MintContainer
import com.example.ui.theme.MintDarkText
import com.example.ui.theme.MintTertiary
import kotlinx.coroutines.delay

private val fallbackQuizWords = listOf("Sun", "Tree", "Book", "Star", "House", "Water", "Cloud")

@Composable
fun QuizExerciseScreen(
    words: List<VocabWord>,
    fallbackEmoji: String,
    onSpeakWord: (String) -> Unit,
    onBack: () -> Unit,
    onFinish: (wordOutcomes: Map<VocabWord, Boolean>, starsEarned: Int, score: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val queue = remember(words) { words.shuffled().take(10) }
    var currentIndex by remember { mutableIntStateOf(0) }
    val currentWord = queue.getOrNull(currentIndex) ?: return

    val options = remember(currentIndex, words) {
        val fromPack = words
            .filter { !it.englishWord.equals(currentWord.englishWord, ignoreCase = true) }
            .map { it.englishWord }
            .distinct()
            .shuffled()
            .take(3)
        val extras = fallbackQuizWords
            .filter { fb ->
                !fb.equals(currentWord.englishWord, ignoreCase = true) &&
                    fromPack.none { it.equals(fb, ignoreCase = true) }
            }
            .shuffled()
        (listOf(currentWord.englishWord) + (fromPack + extras).take(3)).shuffled()
    }

    var selectedOption by remember(currentIndex) { mutableStateOf<String?>(null) }
    var totalMistakes by remember { mutableIntStateOf(0) }
    val outcomes = remember { mutableStateMapOf<VocabWord, Boolean>() }

    LaunchedEffect(selectedOption) {
        val picked = selectedOption ?: return@LaunchedEffect
        val isCorrect = picked.equals(currentWord.englishWord, ignoreCase = true)
        if (!outcomes.containsKey(currentWord)) {
            outcomes[currentWord] = isCorrect
        }
        if (!isCorrect) {
            totalMistakes += 1
        }
        onSpeakWord(currentWord.englishWord)
        delay(950)
        if (currentIndex + 1 < queue.size) {
            currentIndex += 1
        } else {
            val correct = outcomes.values.count { it }
            val total = queue.size.coerceAtLeast(1)
            val pct = (correct * 100) / total
            val stars = when {
                pct >= 85 -> 3
                pct >= 50 -> 2
                else -> 1
            }
            val score = (correct * 25 - totalMistakes * 5).coerceAtLeast(10)
            onFinish(outcomes.toMap(), stars, score)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 760.dp)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("quiz_back_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Kvíz", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Otázka ${currentIndex + 1} z ${queue.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${outcomes.values.count { it }}/${queue.size}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        LinearProgressIndicator(
            progress = { (currentIndex + 1).toFloat() / queue.size.coerceAtLeast(1).toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
        )

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
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
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = currentWord.czechTranslation,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("quiz_czech_word")
                )
            }
        }

        Text(
            text = "Vyber správné anglické slovíčko:",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEachIndexed { idx, optionText ->
                val isSelected = selectedOption == optionText
                val isTarget = optionText.equals(currentWord.englishWord, ignoreCase = true)
                val showResult = selectedOption != null

                val bg by animateColorAsState(
                    targetValue = when {
                        showResult && isTarget -> MintContainer
                        showResult && isSelected && !isTarget -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surface
                    },
                    animationSpec = spring(),
                    label = "quizOptionBg"
                )
                val borderColor = when {
                    showResult && isTarget -> MintTertiary
                    showResult && isSelected && !isTarget -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                }

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = bg),
                    border = BorderStroke(2.dp, borderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(enabled = selectedOption == null) {
                            selectedOption = optionText
                        }
                        .testTag("quiz_option_$idx")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = optionText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                showResult && isTarget -> MintDarkText
                                showResult && isSelected && !isTarget -> MaterialTheme.colorScheme.onErrorContainer
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (showResult && isTarget) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Správná odpověď",
                                    tint = MintTertiary
                                )
                            } else if (showResult && isSelected && !isTarget) {
                                Icon(
                                    imageVector = Icons.Rounded.Cancel,
                                    contentDescription = "Špatná odpověď",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            IconButton(
                                onClick = { onSpeakWord(optionText) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                    contentDescription = "Poslechnout $optionText",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
