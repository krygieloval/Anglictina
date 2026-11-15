package com.example.ui.child

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.VocabWord
import com.example.ui.components.WordIllustrationBox
import com.example.ui.theme.MintContainer
import com.example.ui.theme.MintDarkText
import com.example.ui.theme.MintTertiary
import kotlinx.coroutines.delay

@Composable
fun MatchingExerciseScreen(
    words: List<VocabWord>,
    fallbackEmoji: String,
    onSpeakWord: (String) -> Unit,
    onSpeakCzech: (String) -> Unit,
    onBack: () -> Unit,
    onFinish: (wordOutcomes: Map<VocabWord, Boolean>, starsEarned: Int, score: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val batches = remember(words) { words.shuffled().take(10).chunked(5) }
    var batchIndex by remember { mutableIntStateOf(0) }
    val currentBatch = batches.getOrElse(batchIndex) { emptyList() }

    val leftItems = remember(batchIndex, currentBatch) { currentBatch.shuffled() }
    val rightItems = remember(batchIndex, currentBatch) { currentBatch.shuffled() }

    var selectedLeftId by remember(batchIndex) { mutableStateOf<String?>(null) }
    var selectedRightId by remember(batchIndex) { mutableStateOf<String?>(null) }
    var matchedIds by remember(batchIndex) { mutableStateOf<Set<String>>(emptySet()) }
    var wrongPair by remember(batchIndex) { mutableStateOf<Pair<String, String>?>(null) }
    var totalMistakes by remember { mutableIntStateOf(0) }

    val outcomes = remember { mutableStateMapOf<VocabWord, Boolean>() }

    LaunchedEffect(selectedLeftId, selectedRightId) {
        val left = selectedLeftId
        val right = selectedRightId
        if (left != null && right != null) {
            if (left == right) {
                val word = currentBatch.firstOrNull { it.id == left }
                if (word != null && !outcomes.containsKey(word)) {
                    outcomes[word] = true
                }
                matchedIds = matchedIds + left
                selectedLeftId = null
                selectedRightId = null
                if (word != null) {
                    delay(650)
                    onSpeakWord(word.englishWord)
                }
            } else {
                val word = currentBatch.firstOrNull { it.id == left }
                if (word != null) {
                    outcomes[word] = false
                }
                totalMistakes += 1
                wrongPair = left to right
                delay(550)
                wrongPair = null
                selectedLeftId = null
                selectedRightId = null
            }
        }
    }

    LaunchedEffect(matchedIds, batchIndex) {
        if (currentBatch.isNotEmpty() && matchedIds.size == currentBatch.size) {
            delay(450)
            if (batchIndex + 1 < batches.size) {
                batchIndex += 1
            } else {
                val totalWords = words.size.coerceAtLeast(1)
                val correct = outcomes.values.count { it }
                val stars = when {
                    totalMistakes == 0 -> 3
                    correct * 100 / totalWords >= 60 -> 2
                    else -> 1
                }
                val score = (correct * 20 - totalMistakes * 5).coerceAtLeast(10)
                onFinish(outcomes.toMap(), stars, score)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 820.dp)
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
                    .testTag("matching_back_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Spojovačka",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Kolo ${batchIndex + 1} z ${batches.size.coerceAtLeast(1)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${matchedIds.size}/${currentBatch.size}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        LinearProgressIndicator(
            progress = {
                if (currentBatch.isEmpty()) 0f
                else matchedIds.size.toFloat() / currentBatch.size.toFloat()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
        )

        Text(
            text = "Ťukni na anglické slovíčko vlevo a pak na jeho obrázek a překlad vpravo!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                leftItems.forEach { word ->
                    val isMatched = word.id in matchedIds
                    val isSelected = selectedLeftId == word.id
                    val isWrong = wrongPair?.first == word.id

                    val bg by animateColorAsState(
                        targetValue = when {
                            isMatched -> MintContainer
                            isWrong -> MaterialTheme.colorScheme.errorContainer
                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surface
                        },
                        animationSpec = spring(),
                        label = "leftBg"
                    )
                    val borderColor = when {
                        isMatched -> MintTertiary
                        isWrong -> MaterialTheme.colorScheme.error
                        isSelected -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    }

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = bg),
                        border = BorderStroke(2.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(92.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(enabled = !isMatched) {
                                selectedLeftId = word.id
                                onSpeakWord(word.englishWord)
                            }
                            .testTag("match_left_${word.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = word.englishWord,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isMatched) MintDarkText else MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (isMatched) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Správně spojeno",
                                    tint = MintTertiary
                                )
                            } else {
                                IconButton(
                                    onClick = { onSpeakWord(word.englishWord) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                        contentDescription = "Poslechnout",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rightItems.forEach { word ->
                    val isMatched = word.id in matchedIds
                    val isSelected = selectedRightId == word.id
                    val isWrong = wrongPair?.second == word.id

                    val bg by animateColorAsState(
                        targetValue = when {
                            isMatched -> MintContainer
                            isWrong -> MaterialTheme.colorScheme.errorContainer
                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surface
                        },
                        animationSpec = spring(),
                        label = "rightBg"
                    )
                    val borderColor = when {
                        isMatched -> MintTertiary
                        isWrong -> MaterialTheme.colorScheme.error
                        isSelected -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    }

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = bg),
                        border = BorderStroke(2.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(92.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(enabled = !isMatched) {
                                selectedRightId = word.id
                                onSpeakCzech(word.czechTranslation)
                            }
                            .testTag("match_right_${word.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WordIllustrationBox(
                                cacheKey = word.id,
                                imageBase64 = word.imageBase64,
                                fallbackEmoji = fallbackEmoji,
                                englishWord = word.englishWord,
                                contentDescription = word.czechTranslation,
                                modifier = Modifier.size(68.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = word.czechTranslation,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isMatched) MintDarkText else MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
