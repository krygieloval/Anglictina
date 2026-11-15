package com.example.ui.child

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ExerciseType
import com.example.data.model.VocabWord
import com.example.data.model.WordPack
import com.example.ui.DashboardData
import com.example.ui.PackProgressSummary
import com.example.ui.UiState
import com.example.ui.components.WordIllustrationBox
import com.example.ui.components.packPaletteFor

@Composable
fun ChildHomeScreen(
    dashboard: DashboardData,
    onOpenPack: (WordPack) -> Unit,
    modifier: Modifier = Modifier
) {
    val summariesById = dashboard.analytics.packSummaries.associateBy { it.pack.id }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth > 640.dp
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 920.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Tvoje balíčky slovíček",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (dashboard.packs.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Zatím tu nejsou žádné balíčky",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Popros rodiče, aby v rodičovském režimu vytvořili první balíček slovíček!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                val rows = if (isWide) dashboard.packs.chunked(2) else dashboard.packs.map { listOf(it) }
                items(rows, key = { row -> row.joinToString("_") { it.id } }) { rowPacks ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        for (pack in rowPacks) {
                            val summary = summariesById[pack.id]
                            ChildPackCard(
                                pack = pack,
                                summary = summary,
                                onClick = { onOpenPack(pack) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (isWide && rowPacks.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChildPackCard(
    pack: WordPack,
    summary: PackProgressSummary?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = packPaletteFor(pack.colorIndex)
    val mastered = summary?.masteredWordsCount ?: 0
    val total = pack.wordCount.toInt().coerceAtLeast(1)
    val progress = (mastered.toFloat() / total.toFloat()).coerceIn(0f, 1f)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = palette.container),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .testTag("child_pack_card_${pack.id}")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Column {
                        Text(
                            text = pack.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.onContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${pack.wordCount} slovíček • ${pack.parsedExercises().size} her",
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.onContainer.copy(alpha = 0.78f)
                        )
                    }
                }
                Surface(
                    color = palette.accent,
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Hrát balíček ${pack.title}",
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { progress },
                color = palette.accent,
                trackColor = Color.White.copy(alpha = 0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
            )

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                pack.parsedExercises().forEach { type ->
                    Surface(
                        color = Color.White.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = type.shortTitleCz,
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.onContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChildPackHubScreen(
    pack: WordPack,
    wordsState: UiState<List<VocabWord>>,
    onBack: () -> Unit,
    onSpeakWord: (String) -> Unit,
    onStartExercise: (ExerciseType, List<VocabWord>) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val palette = packPaletteFor(pack.colorIndex)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 880.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                color = palette.container,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("pack_hub_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Zpět na balíčky",
                            tint = palette.onContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = pack.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = palette.onContainer
                        )
                        if (pack.description.isNotBlank()) {
                            Text(
                                text = pack.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.onContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        when (wordsState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Načítám slovíčka...", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = wordsState.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            is UiState.Success -> {
                val words = wordsState.data
                item {
                    Text(
                        text = "Vyber si způsob procvičování",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (words.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                text = "V tomto balíčku zatím nejsou žádná slovíčka. Rodiče je mohou přidat v rodičovském režimu.",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                } else {
                    val enabled = pack.parsedExercises()
                    items(enabled, key = { it.id }) { exerciseType ->
                        val needsMoreWords = (exerciseType == ExerciseType.MATCHING || exerciseType == ExerciseType.MEMORY) && words.size < 2
                        ExerciseLaunchCard(
                            exerciseType = exerciseType,
                            disabledReason = if (needsMoreWords) {
                                "Pro hru ${exerciseType.titleCz} jsou potřeba alespoň 2 slovíčka v balíčku."
                            } else null,
                            onClick = {
                                if (!needsMoreWords) {
                                    onStartExercise(exerciseType, words)
                                }
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Kartičky ke prohlížení (${words.size})",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(words, key = { it.id }) { word ->
                                ChildFlashcard(
                                    word = word,
                                    fallbackEmoji = pack.emoji,
                                    onSpeakEnglish = { onSpeakWord(word.englishWord) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseLaunchCard(
    exerciseType: ExerciseType,
    disabledReason: String?,
    onClick: () -> Unit
) {
    val (icon: ImageVector, bgColor: Color, accentColor: Color) = when (exerciseType) {
        ExerciseType.MATCHING -> Triple(
            Icons.Rounded.Extension,
            Color(0xFFE0F2FE),
            Color(0xFF0284C7)
        )
        ExerciseType.FILL_BLANK -> Triple(
            Icons.Rounded.Edit,
            Color(0xFFFEF3C7),
            Color(0xFFD97706)
        )
        ExerciseType.PRONUNCIATION -> Triple(
            Icons.Rounded.Mic,
            Color(0xFFEDE9FE),
            Color(0xFF7C3AED)
        )
        ExerciseType.SHOOTER -> Triple(
            Icons.Rounded.RocketLaunch,
            Color(0xFFFFE5DF),
            Color(0xFFFF6B4A)
        )
        ExerciseType.MEMORY -> Triple(
            Icons.Rounded.Style,
            Color(0xFFFCE7F3),
            Color(0xFFDB2777)
        )
        ExerciseType.ANAGRAM -> Triple(
            Icons.Rounded.Shuffle,
            Color(0xFFE0E7FF),
            Color(0xFF4F46E5)
        )
        ExerciseType.QUIZ -> Triple(
            Icons.Rounded.Quiz,
            Color(0xFFCCFBF1),
            Color(0xFF0D9488)
        )
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor.copy(alpha = 0.65f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(enabled = disabledReason == null) { onClick() }
            .testTag("launch_exercise_${exerciseType.id.lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.9f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exerciseType.titleCz,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (disabledReason != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = disabledReason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Surface(
                color = if (disabledReason == null) accentColor else MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Spustit ${exerciseType.titleCz}",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChildFlashcard(
    word: VocabWord,
    fallbackEmoji: String,
    onSpeakEnglish: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .width(190.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable { onSpeakEnglish() }
            .testTag("flashcard_${word.id}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WordIllustrationBox(
                cacheKey = word.id,
                imageBase64 = word.imageBase64,
                fallbackEmoji = fallbackEmoji,
                englishWord = word.englishWord,
                contentDescription = word.czechTranslation,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(144.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = word.czechTranslation,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("flashcard_czech_${word.id}")
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable { onSpeakEnglish() }
                    .testTag("flashcard_english_${word.id}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "Poslechnout ${word.englishWord}",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = word.englishWord,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
