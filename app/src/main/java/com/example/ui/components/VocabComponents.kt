package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChildProfile
import com.example.data.model.ParentSettings
import com.example.ui.AppMode
import com.example.ui.theme.PackPalettes
import com.example.ui.theme.SunnyAmber
import com.example.ui.theme.SunnyAmberContainer
import com.example.ui.theme.SunnyAmberText
import com.example.util.ImageCodec
import com.example.util.WordVisuals
import kotlin.random.Random

@Composable
fun WordIllustrationBox(
    cacheKey: String,
    imageBase64: String,
    fallbackEmoji: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    englishWord: String = "",
    czechTranslation: String = contentDescription,
    isLoading: Boolean = false
) {
    val bitmap = remember(cacheKey, imageBase64) {
        if (imageBase64.isNotBlank()) ImageCodec.decodeFromBase64(cacheKey, imageBase64) else null
    }
    val bundledDrawableRes = remember(englishWord, czechTranslation) {
        if (englishWord.isNotBlank()) {
            WordVisuals.drawableResFor(englishWord, czechTranslation)
        } else {
            null
        }
    }
    val resolvedEmoji = remember(englishWord, fallbackEmoji) {
        if (englishWord.isNotBlank()) WordVisuals.emojiFor(englishWord, fallbackEmoji)
        else fallbackEmoji.ifBlank { "🖼️" }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (bundledDrawableRes != null && bitmap == null) {
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFBF8F1),
                            Color(0xFFF6F2E9)
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        )
                    )
                }
            )
            .border(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(18.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Načítám...",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            bitmap != null -> {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            bundledDrawableRes != null -> {
                Image(
                    painter = painterResource(id = bundledDrawableRes),
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val minDim = minOf(maxWidth.value, maxHeight.value)
                    val factor = if (resolvedEmoji.length >= 2 && resolvedEmoji.all { it.isDigit() }) {
                        0.44f
                    } else {
                        0.60f
                    }
                    val emojiSp = (minDim * factor).coerceIn(22f, 128f).sp
                    Text(
                        text = resolvedEmoji,
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            fontSize = emojiSp,
                            lineHeight = emojiSp,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SlovickaTopHeader(
    activeChild: ChildProfile,
    currentMode: AppMode,
    onOpenProfileSwitcher: () -> Unit,
    onSwitchModeRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profilePalette = packPaletteFor(activeChild.colorIndex)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentMode == AppMode.CHILD) {
                Surface(
                    color = profilePalette.container.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onOpenProfileSwitcher() }
                        .testTag("child_profile_switcher_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color.White.copy(alpha = 0.85f),
                            shape = CircleShape,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = activeChild.avatarEmoji,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Ahoj, ${activeChild.name}!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = profilePalette.onContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = "Přepnout profil dítěte",
                                    tint = profilePalette.onContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "Vyber si balíček a pojď si hrát",
                                style = MaterialTheme.typography.bodySmall,
                                color = profilePalette.onContainer.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Rodičovský režim",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Aktivní profil: ${activeChild.avatarEmoji} ${activeChild.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = SunnyAmberContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = "Hvězdičky",
                            tint = SunnyAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = activeChild.totalStars.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = SunnyAmberText
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("mode_switch_button")
                ) {
                    Row(
                        modifier = Modifier.padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isChildMode = currentMode == AppMode.CHILD
                        Surface(
                            color = if (isChildMode) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.Transparent
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    if (!isChildMode) onSwitchModeRequested()
                                }
                                .testTag("mode_switch_child")
                        ) {
                            Text(
                                text = "Dítě",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isChildMode) FontWeight.Bold else FontWeight.Medium,
                                color = if (isChildMode) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }
                        Surface(
                            color = if (!isChildMode) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                Color.Transparent
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    if (isChildMode) onSwitchModeRequested()
                                }
                                .testTag("mode_switch_parent")
                        ) {
                            Text(
                                text = "Rodič",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (!isChildMode) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isChildMode) {
                                    MaterialTheme.colorScheme.onSecondary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChildProfileSwitcherDialog(
    profiles: List<ChildProfile>,
    activeChildId: String,
    onSelectProfile: (ChildProfile) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Kdo teď hraje?",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                profiles.forEach { profile ->
                    val isActive = profile.id == activeChildId
                    val palette = packPaletteFor(profile.colorIndex)
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) palette.container else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        ),
                        border = if (isActive) BorderStroke(2.dp, palette.accent) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onSelectProfile(profile) }
                            .testTag("switch_profile_item_${profile.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.85f),
                                shape = CircleShape,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(profile.avatarEmoji, style = MaterialTheme.typography.headlineSmall)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${profile.totalStars} hvězdiček",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isActive) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Aktivní profil",
                                    tint = palette.accent
                                )
                            }
                        }
                    }
                }
                Text(
                    text = "Nové profily dětí můžete přidat nebo upravit v Rodičovském režimu → Nastavení.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Zavřít")
            }
        }
    )
}

@Composable
fun SpeakButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(50),
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = "Přehrát anglickou výslovnost",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            if (!label.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun ParentalGateDialog(
    settings: ParentSettings,
    onUnlocked: () -> Unit,
    onDismiss: () -> Unit
) {
    val usePin = settings.parentPinEnabled && settings.parentPin.isNotBlank()
    val mathPair = remember {
        val a = Random.nextInt(4, 10)
        val b = Random.nextInt(3, 9)
        a to b
    }
    val expectedAnswer = if (usePin) settings.parentPin else (mathPair.first * mathPair.second).toString()
    var input by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Vstup pro rodiče",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (usePin) {
                        "Zadejte prosím rodičovský PIN kód:"
                    } else {
                        "Ověření pro rodiče: Kolik je ${mathPair.first} × ${mathPair.second}?"
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it.filter { ch -> ch.isDigit() }.take(6)
                        errorText = null
                    },
                    label = { Text(if (usePin) "Rodičovský PIN" else "Výsledek") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = errorText != null,
                    supportingText = errorText?.let { err -> { Text(err) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("parent_gate_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (input.trim() == expectedAnswer) {
                        onUnlocked()
                    } else {
                        errorText = if (usePin) "Nesprávný PIN kód." else "Nesprávný výsledek, zkuste to znovu."
                    }
                },
                modifier = Modifier.testTag("parent_gate_confirm")
            ) {
                Text("Odemknout")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zpět")
            }
        }
    )
}

fun packPaletteFor(colorIndex: Long) =
    PackPalettes[(colorIndex.toInt().coerceAtLeast(0)) % PackPalettes.size]
