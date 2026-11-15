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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.VocabWord
import com.example.ui.components.SpeakButton
import com.example.ui.components.WordIllustrationBox
import com.example.ui.theme.MintContainer
import com.example.ui.theme.MintDarkText
import com.example.ui.theme.MintTertiary
import kotlinx.coroutines.delay
import kotlin.random.Random

private data class LetterTile(
    val index: Int,
    val char: Char
)

internal fun prefillCountForLength(wordLength: Int): Int = when {
    wordLength <= 1 -> 0
    wordLength <= 5 -> 1
    else -> 2
}

internal fun choosePrefilledIndices(
    wordLength: Int,
    random: Random = Random.Default
): Set<Int> {
    val count = prefillCountForLength(wordLength)
    if (count == 0) return emptySet()
    return (0 until wordLength).shuffled(random).take(count).toSet()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FillBlankExerciseScreen(
    words: List<VocabWord>,
    fallbackEmoji: String,
    onSpeakWord: (String) -> Unit,
    onSpeakCzech: (String) -> Unit,
    onBack: () -> Unit,
    onFinish: (wordOutcomes: Map<VocabWord, Boolean>, starsEarned: Int, score: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val queue = remember(words) { words.shuffled().take(10) }
    var currentIndex by remember { mutableIntStateOf(0) }
    val currentWord = queue.getOrNull(currentIndex) ?: return

    var useKeyboardMode by remember { mutableStateOf(false) }
    var typedInput by remember(currentIndex) { mutableStateOf("") }

    val targetClean = remember(currentWord) {
        currentWord.englishWord.trim().uppercase()
    }
    val targetLettersOnly = remember(targetClean) {
        targetClean.filter { it.isLetter() }
    }

    val prefilledIndices = remember(currentIndex, targetLettersOnly) {
        choosePrefilledIndices(targetLettersOnly.length)
    }
    val openSlots = remember(targetLettersOnly, prefilledIndices) {
        (0 until targetLettersOnly.length).filter { it !in prefilledIndices }
    }

    val tileBank = remember(currentIndex, targetLettersOnly, openSlots) {
        val alphabet = "AEIOURSTLNMDPCBG"
        val extras = List(3) { alphabet.random() }
        val neededLetters = openSlots.map { targetLettersOnly[it] }
        (neededLetters + extras)
            .shuffled()
            .mapIndexed { idx, ch -> LetterTile(idx, ch) }
    }

    var placedTiles by remember(currentIndex) { mutableStateOf<Map<Int, LetterTile>>(emptyMap()) }
    var hadMistakeOnCurrent by remember(currentIndex) { mutableStateOf(false) }
    var feedbackMessage by remember(currentIndex) { mutableStateOf<String?>(null) }
    var isWordSolved by remember(currentIndex) { mutableStateOf(false) }
    val outcomes = remember { mutableStateMapOf<VocabWord, Boolean>() }

    fun buildCandidate(currentPlaced: Map<Int, LetterTile>): String =
        (0 until targetLettersOnly.length).joinToString("") { idx ->
            if (idx in prefilledIndices) {
                targetLettersOnly[idx].toString()
            } else {
                currentPlaced[idx]?.char?.toString() ?: ""
            }
        }

    fun submitAnswer(candidateRaw: String) {
        val cleanCandidate = candidateRaw.trim().uppercase().filter { it.isLetter() }
        if (cleanCandidate == targetLettersOnly) {
            if (!outcomes.containsKey(currentWord)) {
                outcomes[currentWord] = !hadMistakeOnCurrent
            }
            isWordSolved = true
            feedbackMessage = "Výborně! Správně!"
            onSpeakWord(currentWord.englishWord)
        } else {
            hadMistakeOnCurrent = true
            outcomes[currentWord] = false
            feedbackMessage = "Zkus to ještě opravit!"
        }
    }

    LaunchedEffect(isWordSolved) {
        if (isWordSolved) {
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
                    .testTag("fill_blank_back_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Doplňovačka", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Slovíčko ${currentIndex + 1} z ${queue.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilterChip(
                selected = useKeyboardMode,
                onClick = { useKeyboardMode = !useKeyboardMode },
                label = { Text(if (useKeyboardMode) "Klávesnice" else "Písmenka") },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Keyboard,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier.testTag("toggle_input_mode_chip")
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
                    modifier = Modifier.size(172.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSpeakCzech(currentWord.czechTranslation) }
                        .testTag("fill_blank_czech_word")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = currentWord.czechTranslation,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = "Poslechnout česky",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                SpeakButton(
                    onClick = { onSpeakWord(currentWord.englishWord) },
                    label = "Poslechnout si slovo"
                )
            }
        }

        if (useKeyboardMode) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isWordSolved) MintContainer else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    2.dp,
                    if (isWordSolved) MintTertiary else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(targetLettersOnly.length) { slotIdx ->
                        val isPrefilled = slotIdx in prefilledIndices
                        Surface(
                            color = if (isPrefilled) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.5.dp,
                                if (isPrefilled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (isPrefilled) targetLettersOnly[slotIdx].toString() else "_",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPrefilled) {
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }
            OutlinedTextField(
                value = typedInput,
                onValueChange = {
                    typedInput = it
                    feedbackMessage = null
                },
                label = { Text("Napiš anglické slovíčko") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submitAnswer(typedInput) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("fill_blank_keyboard_input")
            )
            Button(
                onClick = { submitAnswer(typedInput) },
                enabled = typedInput.isNotBlank() && !isWordSolved,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("fill_blank_submit_button")
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Zkontrolovat", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            // Letter slots
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isWordSolved) MintContainer else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    2.dp,
                    if (isWordSolved) MintTertiary else MaterialTheme.colorScheme.outline
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
                    repeat(targetLettersOnly.length) { slotIdx ->
                        val isPrefilled = slotIdx in prefilledIndices
                        val picked = placedTiles[slotIdx]
                        Surface(
                            color = when {
                                isPrefilled -> MaterialTheme.colorScheme.tertiaryContainer
                                picked != null -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.5.dp,
                                when {
                                    isPrefilled -> MaterialTheme.colorScheme.tertiary
                                    picked != null -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outline
                                }
                            ),
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !isPrefilled && picked != null && !isWordSolved) {
                                    placedTiles = placedTiles - slotIdx
                                    feedbackMessage = null
                                }
                                .testTag("fill_blank_slot_$slotIdx")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = when {
                                        isPrefilled -> targetLettersOnly[slotIdx].toString()
                                        picked != null -> picked.char.toString()
                                        else -> "_"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isWordSolved -> MintDarkText
                                        isPrefilled -> MaterialTheme.colorScheme.onTertiaryContainer
                                        picked != null -> MaterialTheme.colorScheme.onPrimaryContainer
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Available tiles
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                tileBank.forEach { tile ->
                    val isUsed = placedTiles.values.any { it.index == tile.index }
                    Surface(
                        color = if (isUsed) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.5.dp,
                            if (isUsed) Color.Transparent else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        ),
                        shadowElevation = if (isUsed) 0.dp else 2.dp,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = !isUsed && !isWordSolved) {
                                val nextSlot = openSlots.firstOrNull { it !in placedTiles }
                                if (nextSlot != null) {
                                    val next = placedTiles + (nextSlot to tile)
                                    placedTiles = next
                                    feedbackMessage = null
                                    if (next.size == openSlots.size) {
                                        submitAnswer(buildCandidate(next))
                                    }
                                }
                            }
                            .testTag("letter_tile_${tile.index}")
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
                        if (!isWordSolved) {
                            val lastFilledSlot = openSlots.lastOrNull { it in placedTiles }
                            if (lastFilledSlot != null) {
                                placedTiles = placedTiles - lastFilledSlot
                                feedbackMessage = null
                            }
                        }
                    },
                    enabled = placedTiles.isNotEmpty() && !isWordSolved,
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
                        if (!isWordSolved) {
                            val targetSlot = openSlots.firstOrNull { it !in placedTiles }
                                ?: openSlots.firstOrNull { placedTiles[it]?.char != targetLettersOnly[it] }
                            if (targetSlot != null) {
                                val neededChar = targetLettersOnly[targetSlot]
                                val unusedMatch = tileBank.firstOrNull { tile ->
                                    tile.char == neededChar && placedTiles.values.none { it.index == tile.index }
                                }
                                val next = if (unusedMatch != null) {
                                    placedTiles + (targetSlot to unusedMatch)
                                } else {
                                    val sourceSlot = openSlots.firstOrNull { s ->
                                        s != targetSlot &&
                                            placedTiles[s]?.char == neededChar &&
                                            placedTiles[s]?.char != targetLettersOnly[s]
                                    } ?: openSlots.firstOrNull { s ->
                                        s != targetSlot && placedTiles[s]?.char == neededChar
                                    }
                                    if (sourceSlot != null) {
                                        val movedTile = placedTiles.getValue(sourceSlot)
                                        (placedTiles - sourceSlot) + (targetSlot to movedTile)
                                    } else {
                                        null
                                    }
                                }
                                if (next != null) {
                                    hadMistakeOnCurrent = true
                                    placedTiles = next
                                    feedbackMessage = null
                                    if (next.size == openSlots.size) {
                                        submitAnswer(buildCandidate(next))
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isWordSolved,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("fill_blank_hint_button")
                ) {
                    Icon(Icons.Rounded.Lightbulb, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Nápověda")
                }
            }
        }

        if (feedbackMessage != null) {
            Surface(
                color = if (isWordSolved) MintContainer else MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = feedbackMessage!!,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isWordSolved) MintDarkText else MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}
