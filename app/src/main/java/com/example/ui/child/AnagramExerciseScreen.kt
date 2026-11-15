package com.example.ui.child

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Shuffle
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
import androidx.compose.ui.unit.dp
import com.example.data.model.VocabWord
import com.example.ui.components.SpeakButton
import com.example.ui.components.WordIllustrationBox
import com.example.ui.theme.MintContainer
import com.example.ui.theme.MintDarkText
import com.example.ui.theme.MintTertiary
import kotlinx.coroutines.delay

private data class AnagramTile(
    val id: Int,
    val char: Char
)

private fun buildScrambledTiles(target: String): List<AnagramTile> {
    val chars = target.toList()
    var scrambled = chars.shuffled()
    if (chars.toSet().size > 1) {
        var guard = 0
        while (scrambled == chars && guard < 8) {
            scrambled = chars.shuffled()
            guard++
        }
        if (scrambled == chars) {
            scrambled = chars.drop(1) + chars.first()
        }
    }
    return scrambled.mapIndexed { idx, ch -> AnagramTile(idx, ch) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnagramExerciseScreen(
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

    val targetLetters = remember(currentWord) {
        currentWord.englishWord.trim().uppercase().filter { it.isLetter() }
    }

    var tiles by remember(currentIndex, targetLetters) {
        mutableStateOf(buildScrambledTiles(targetLetters))
    }
    var pickedTiles by remember(currentIndex) { mutableStateOf<List<AnagramTile>>(emptyList()) }
    var hadMistake by remember(currentIndex) { mutableStateOf(false) }
    var feedbackMessage by remember(currentIndex) { mutableStateOf<String?>(null) }
    var isSolved by remember(currentIndex) { mutableStateOf(false) }
    val outcomes = remember { mutableStateMapOf<VocabWord, Boolean>() }

    fun checkCompletion(nextPicked: List<AnagramTile>) {
        if (nextPicked.size < targetLetters.length) return
        val candidate = nextPicked.map { it.char }.joinToString("")
        if (candidate == targetLetters) {
            if (!outcomes.containsKey(currentWord)) {
                outcomes[currentWord] = !hadMistake
            }
            isSolved = true
            feedbackMessage = "Skvělé! Přesmyčka je správně!"
            onSpeakWord(currentWord.englishWord)
        } else {
            hadMistake = true
            outcomes[currentWord] = false
            feedbackMessage = "Tohle není správné pořadí písmen. Zkus to znovu!"
        }
    }

    LaunchedEffect(isSolved) {
        if (isSolved) {
            delay(1000)
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
                onFinish(outcomes.toMap(), stars, correct * 25)
            }
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
                    .testTag("anagram_back_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Anagram", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Slovíčko ${currentIndex + 1} z ${queue.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = {
                    if (!isSolved) {
                        tiles = buildScrambledTiles(targetLetters)
                        pickedTiles = emptyList()
                        feedbackMessage = null
                    }
                },
                modifier = Modifier.height(44.dp)
            ) {
                Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Zamíchat")
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
                    modifier = Modifier.size(172.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = currentWord.czechTranslation,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("anagram_czech_word")
                )
                Spacer(modifier = Modifier.height(10.dp))
                SpeakButton(
                    onClick = { onSpeakWord(currentWord.englishWord) },
                    label = "Poslechnout si slovo"
                )
            }
        }

        // Answer slots
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSolved) MintContainer else MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(
                2.dp,
                if (isSolved) MintTertiary else MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(targetLetters.length) { slotIdx ->
                    val picked = pickedTiles.getOrNull(slotIdx)
                    Surface(
                        color = if (picked != null) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.5.dp,
                            if (picked != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = picked != null && !isSolved) {
                                if (picked != null) {
                                    pickedTiles = pickedTiles - picked
                                    feedbackMessage = null
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = picked?.char?.toString() ?: "",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isSolved) MintDarkText else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Scrambled letter tiles
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            tiles.forEach { tile ->
                val isUsed = pickedTiles.any { it.id == tile.id }
                Surface(
                    color = if (isUsed) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.5.dp,
                        if (isUsed) Color.Transparent else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    ),
                    shadowElevation = if (isUsed) 0.dp else 2.dp,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(enabled = !isUsed && !isSolved) {
                            if (pickedTiles.size < targetLetters.length) {
                                val next = pickedTiles + tile
                                pickedTiles = next
                                feedbackMessage = null
                                checkCompletion(next)
                            }
                        }
                        .testTag("anagram_tile_${tile.id}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isUsed) "" else tile.char.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    if (pickedTiles.isNotEmpty() && !isSolved) {
                        pickedTiles = pickedTiles.dropLast(1)
                        feedbackMessage = null
                    }
                },
                enabled = pickedTiles.isNotEmpty() && !isSolved,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Smazat")
            }

            OutlinedButton(
                onClick = {
                    if (!isSolved && pickedTiles.size < targetLetters.length) {
                        val nextChar = targetLetters[pickedTiles.size]
                        val matchingTile = tiles.firstOrNull { tile ->
                            tile.char == nextChar && pickedTiles.none { it.id == tile.id }
                        }
                        if (matchingTile != null) {
                            hadMistake = true
                            val next = pickedTiles + matchingTile
                            pickedTiles = next
                            checkCompletion(next)
                        }
                    }
                },
                enabled = !isSolved,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("anagram_hint_button")
            ) {
                Icon(Icons.Rounded.Lightbulb, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Nápověda")
            }
        }

        if (feedbackMessage != null) {
            Surface(
                color = if (isSolved) MintContainer else MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = feedbackMessage!!,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSolved) MintDarkText else MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}
