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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.VocabWord
import com.example.ui.components.SpeakButton
import com.example.ui.components.WordIllustrationBox
import com.example.ui.theme.MintContainer
import com.example.ui.theme.MintDarkText
import com.example.ui.theme.MintTertiary
import kotlinx.coroutines.delay

private enum class MemoryCardKind {
    ENGLISH,
    PICTURE
}

private data class MemoryCard(
    val uid: String,
    val word: VocabWord,
    val kind: MemoryCardKind
)

@Composable
fun MemoryGameScreen(
    words: List<VocabWord>,
    fallbackEmoji: String,
    onSpeakWord: (String) -> Unit,
    onSpeakCzech: (String) -> Unit,
    onBack: () -> Unit,
    onFinish: (wordOutcomes: Map<VocabWord, Boolean>, starsEarned: Int, score: Int, movesCount: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val roundWords = remember(words) { words.shuffled().take(6) }
    val cards = remember(roundWords) {
        roundWords.flatMap { word ->
            listOf(
                MemoryCard("${word.id}_en", word, MemoryCardKind.ENGLISH),
                MemoryCard("${word.id}_pic", word, MemoryCardKind.PICTURE)
            )
        }.shuffled()
    }

    var flippedUids by remember(cards) { mutableStateOf<List<String>>(emptyList()) }
    var matchedWordIds by remember(cards) { mutableStateOf<Set<String>>(emptySet()) }
    var isCheckingPair by remember(cards) { mutableStateOf(false) }
    var movesCount by remember(cards) { mutableIntStateOf(0) }
    var lastSpokenWord by remember(cards) { mutableStateOf<VocabWord?>(null) }

    val outcomes = remember(cards) { mutableStateMapOf<VocabWord, Boolean>() }

    LaunchedEffect(flippedUids) {
        if (flippedUids.size == 2) {
            isCheckingPair = true
            movesCount += 1
            val first = cards.firstOrNull { it.uid == flippedUids[0] }
            val second = cards.firstOrNull { it.uid == flippedUids[1] }
            if (first != null && second != null) {
                if (first.word.id == second.word.id) {
                    delay(280)
                    matchedWordIds = matchedWordIds + first.word.id
                    outcomes[first.word] = true
                    delay(350)
                    onSpeakWord(first.word.englishWord)
                    lastSpokenWord = first.word
                    flippedUids = emptyList()
                    isCheckingPair = false
                } else {
                    delay(800)
                    flippedUids = emptyList()
                    isCheckingPair = false
                }
            } else {
                flippedUids = emptyList()
                isCheckingPair = false
            }
        }
    }

    LaunchedEffect(matchedWordIds) {
        if (roundWords.isNotEmpty() && matchedWordIds.size == roundWords.size) {
            delay(750)
            val totalPairs = roundWords.size.coerceAtLeast(1)
            val stars = when {
                movesCount <= totalPairs + 2 -> 3
                movesCount <= totalPairs * 2 -> 2
                else -> 1
            }
            val score = (totalPairs * 30 - (movesCount - totalPairs).coerceAtLeast(0) * 4).coerceAtLeast(20)
            onFinish(outcomes.toMap(), stars, score, movesCount)
        }
    }

    val columns = if (cards.size <= 4) 2 else 3
    val rows = remember(cards, columns) { cards.chunked(columns) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 820.dp)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("memory_back_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Pexeso", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Spoj anglické slovíčko s obrázkem",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${matchedWordIds.size}/${roundWords.size} • Tahů: $movesCount",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        LinearProgressIndicator(
            progress = { matchedWordIds.size.toFloat() / roundWords.size.coerceAtLeast(1).toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
        )

        lastSpokenWord?.let { word ->
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = word.englishWord,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = word.czechTranslation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    SpeakButton(
                        onClick = { onSpeakWord(word.englishWord) },
                        label = "Přehrát"
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEach { rowCards ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (card in rowCards) {
                        val isMatched = card.word.id in matchedWordIds
                        val isFaceUp = isMatched || card.uid in flippedUids

                        val bg by animateColorAsState(
                            targetValue = when {
                                isMatched -> MintContainer
                                isFaceUp -> MaterialTheme.colorScheme.surface
                                else -> MaterialTheme.colorScheme.primaryContainer
                            },
                            animationSpec = spring(),
                            label = "pexesoCardBg"
                        )
                        val borderColor = when {
                            isMatched -> MintTertiary
                            isFaceUp -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        }

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = bg),
                            border = BorderStroke(2.dp, borderColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isFaceUp) 3.dp else 1.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(148.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable(enabled = !isCheckingPair) {
                                    if (!isFaceUp && flippedUids.size < 2) {
                                        flippedUids = flippedUids + card.uid
                                        if (card.kind == MemoryCardKind.ENGLISH) {
                                            onSpeakWord(card.word.englishWord)
                                        } else {
                                            onSpeakCzech(card.word.czechTranslation)
                                        }
                                        lastSpokenWord = card.word
                                    } else if (isFaceUp) {
                                        if (card.kind == MemoryCardKind.ENGLISH) {
                                            onSpeakWord(card.word.englishWord)
                                        } else {
                                            onSpeakCzech(card.word.czechTranslation)
                                        }
                                        lastSpokenWord = card.word
                                    }
                                }
                                .testTag("memory_card_${card.uid}")
                        ) {
                            if (!isFaceUp) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Rounded.AutoAwesome,
                                            contentDescription = "Otočit kartičku",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "?",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            } else if (card.kind == MemoryCardKind.ENGLISH) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                ) {
                                    if (isMatched) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = "Nalezeno",
                                            tint = MintTertiary,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .align(Alignment.TopEnd)
                                        )
                                    }
                                    Column(
                                        modifier = Modifier.align(Alignment.Center),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = card.word.englishWord,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMatched) MintDarkText else MaterialTheme.colorScheme.primary,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                            contentDescription = "Poslechnout",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    WordIllustrationBox(
                                        cacheKey = card.word.id,
                                        imageBase64 = card.word.imageBase64,
                                        fallbackEmoji = fallbackEmoji,
                                        englishWord = card.word.englishWord,
                                        contentDescription = card.word.czechTranslation,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = card.word.czechTranslation,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isMatched) MintDarkText else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    repeat(columns - rowCards.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
