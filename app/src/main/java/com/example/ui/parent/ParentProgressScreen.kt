package com.example.ui.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.DashboardData
import com.example.ui.ExerciseStatSummary
import com.example.ui.PackProgressSummary
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CoralDarkText
import com.example.ui.theme.CoralSecondary
import com.example.ui.theme.MintContainer
import com.example.ui.theme.MintDarkText
import com.example.ui.theme.MintTertiary
import com.example.ui.theme.SunnyAmber
import com.example.ui.theme.SunnyAmberContainer
import com.example.ui.theme.SunnyAmberText

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParentProgressScreen(
    dashboard: DashboardData,
    onSelectChild: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val analytics = dashboard.analytics

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 880.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Pokrok dítěte: ${dashboard.activeChild.avatarEmoji} ${dashboard.activeChild.name}",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Přehled naučených slovíček, úspěšnosti cvičení a oblastí k dopilování.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (dashboard.childProfiles.size > 1) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    dashboard.childProfiles.forEach { profile ->
                        FilterChip(
                            selected = profile.id == dashboard.activeChild.id,
                            onClick = { onSelectChild(profile.id) },
                            label = { Text("${profile.avatarEmoji} ${profile.name}") }
                        )
                    }
                }
            }
        }

        // Top KPI cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiMetricCard(
                    title = "Naučeno slov",
                    value = "${analytics.learnedWordsCount}/${analytics.totalWordsCount}",
                    bgColor = MintContainer,
                    textColor = MintDarkText,
                    icon = Icons.Rounded.CheckCircle,
                    iconTint = MintTertiary,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    title = "Úspěšnost",
                    value = "${analytics.overallAccuracyPercent} %",
                    bgColor = MaterialTheme.colorScheme.primaryContainer,
                    textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    icon = Icons.Rounded.TrendingUp,
                    iconTint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    title = "Hvězdičky",
                    value = dashboard.activeChild.totalStars.toString(),
                    bgColor = SunnyAmberContainer,
                    textColor = SunnyAmberText,
                    icon = Icons.Rounded.Star,
                    iconTint = SunnyAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Success rate per exercise type
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Úspěšnost podle typu cvičení",
                        style = MaterialTheme.typography.titleLarge
                    )
                    analytics.exerciseBreakdown.forEach { item ->
                        ExerciseBreakdownRow(item)
                    }
                }
            }
        }

        // Weak areas / words with gaps
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = CoralSecondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Kde má dítě mezery (k procvičení)",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    if (analytics.weakWords.isEmpty()) {
                        Text(
                            text = if (analytics.practicedWordsCount == 0) {
                                "Zatím nebyla procvičena žádná slovíčka. Po prvních hrách se zde ukážou problémová slova."
                            } else {
                                "Skvělé! Momentálně nemá dítě u procvičených slovíček výraznější mezery."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        analytics.weakWords.take(8).forEach { wp ->
                            Surface(
                                color = CoralContainer.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "${wp.englishWord} — ${wp.czechTranslation}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = CoralDarkText
                                        )
                                        Text(
                                            text = "${wp.correctCount} z ${wp.attempts} správně",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = CoralDarkText.copy(alpha = 0.8f)
                                        )
                                    }
                                    Surface(
                                        color = CoralSecondary,
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = "${wp.accuracyPercent} %",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Pack-by-pack summary
        if (analytics.packSummaries.isNotEmpty()) {
            item {
                Text(
                    text = "Úspěšnost podle balíčků",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            items(analytics.packSummaries, key = { it.pack.id }) { summary ->
                PackSummaryRow(summary)
            }
        }
    }
}

@Composable
private fun KpiMetricCard(
    title: String,
    value: String,
    bgColor: Color,
    textColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = textColor.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun ExerciseBreakdownRow(item: ExerciseStatSummary) {
    val isMemory = item.type == com.example.data.model.ExerciseType.MEMORY
    val hasActivity = if (isMemory) item.sessionsCompleted > 0L else item.attempts > 0L
    val progress = when {
        !hasActivity -> 0f
        isMemory -> 1f
        else -> (item.accuracyPercent / 100f).coerceIn(0f, 1f)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${item.type.titleCz} (${item.type.skillTagCz})",
                    style = MaterialTheme.typography.titleSmall
                )
                val subtitle = when {
                    !hasActivity -> "Zatím neprocvičeno"
                    isMemory -> {
                        val bestPart = if (item.bestScore > 0L) " • Nejlepší skóre: ${item.bestScore}" else ""
                        "${item.sessionsCompleted} dokončených her$bestPart"
                    }
                    else -> {
                        val bestPart = if (item.bestScore > 0L) " • Nejlepší skóre: ${item.bestScore}" else ""
                        "${item.sessionsCompleted} her • ${item.correctCount}/${item.attempts} správně$bestPart"
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = when {
                    !hasActivity -> "—"
                    isMemory -> "${item.sessionsCompleted}×"
                    else -> "${item.accuracyPercent} %"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            color = when {
                !hasActivity -> MaterialTheme.colorScheme.outline
                isMemory || item.accuracyPercent >= 75 -> MintTertiary
                item.accuracyPercent >= 50 -> SunnyAmber
                else -> CoralSecondary
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
        )
    }
}

@Composable
private fun PackSummaryRow(summary: PackProgressSummary) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(summary.pack.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Naučeno: ${summary.masteredWordsCount} z ${summary.pack.wordCount} slovíček",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = if (summary.practicedWordsCount == 0) "Nové" else "${summary.accuracyPercent} %",
                style = MaterialTheme.typography.titleMedium,
                color = if (summary.practicedWordsCount == 0) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else if (summary.accuracyPercent >= 75) {
                    MintTertiary
                } else {
                    CoralSecondary
                }
            )
        }
    }
}
