package com.example.ui.parent

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.ChildProfile
import com.example.data.model.ParentSettings
import com.example.ui.components.packPaletteFor
import com.example.ui.theme.PackPalettes

private val CHILD_AVATARS = listOf("🦁", "🦊", "🐼", "🐰", "🐸", "🦄", "🚀", "🌟", "🐱", "🐶")

@Composable
fun ParentSettingsScreen(
    settings: ParentSettings,
    childProfiles: List<ChildProfile>,
    activeChildId: String,
    onSwitchChild: (String) -> Unit,
    onCreateChild: (name: String, avatarEmoji: String, colorIndex: Long) -> Unit,
    onUpdateChild: (childId: String, name: String, avatarEmoji: String, colorIndex: Long) -> Unit,
    onDeleteChild: (String) -> Unit,
    onSaveSettings: (pinEnabled: Boolean, pin: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var pinEnabled by remember(settings.parentPinEnabled) { mutableStateOf(settings.parentPinEnabled) }
    var pin by remember(settings.parentPin) { mutableStateOf(settings.parentPin) }
    var pinError by remember { mutableStateOf<String?>(null) }

    var showCreateProfileDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<ChildProfile?>(null) }
    var deletingProfile by remember { mutableStateOf<ChildProfile?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 720.dp)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Nastavení pro rodiče",
            style = MaterialTheme.typography.headlineSmall
        )

        // Child profiles management card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Profily dětí (${childProfiles.size})",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Každé dítě má vlastní hvězdičky a statistiku pokroku.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = { showCreateProfileDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("add_child_profile_button")
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Přidat dítě")
                    }
                }

                childProfiles.forEach { profile ->
                    val isActive = profile.id == activeChildId
                    val palette = packPaletteFor(profile.colorIndex)
                    Surface(
                        color = if (isActive) palette.container.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(18.dp),
                        border = if (isActive) BorderStroke(1.5.dp, palette.accent) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onSwitchChild(profile.id) }
                            .testTag("settings_child_row_${profile.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = profile.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isActive) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = "Aktivní profil",
                                            tint = palette.accent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${profile.totalStars} hvězdiček",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { editingProfile = profile },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("edit_child_${profile.id}")
                            ) {
                                Icon(
                                    Icons.Rounded.Edit,
                                    contentDescription = "Upravit profil",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (childProfiles.size > 1) {
                                IconButton(
                                    onClick = { deletingProfile = profile },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("delete_child_${profile.id}")
                                ) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = "Smazat profil",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Security PIN settings card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Zabezpečení rodičovského režimu",
                    style = MaterialTheme.typography.titleLarge
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Zamknout rodičovský režim PINem",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Pokud je vypnuto, použije se rychlý matematický příklad.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = pinEnabled,
                        onCheckedChange = {
                            pinEnabled = it
                            pinError = null
                        },
                        modifier = Modifier.testTag("settings_pin_switch")
                    )
                }

                if (pinEnabled) {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = {
                            pin = it.filter { ch -> ch.isDigit() }.take(6)
                            pinError = null
                        },
                        label = { Text("Rodičovský PIN (4–6 číslic)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = pinError != null,
                        supportingText = pinError?.let { err -> { Text(err) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_pin_input")
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        if (pinEnabled && pin.length < 4) {
                            pinError = "PIN musí mít alespoň 4 číslice."
                        } else {
                            onSaveSettings(pinEnabled, pin)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("settings_save_button")
                ) {
                    Text("Uložit zabezpečení", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    if (showCreateProfileDialog || editingProfile != null) {
        ChildProfileEditorDialog(
            initialProfile = editingProfile,
            onDismiss = {
                showCreateProfileDialog = false
                editingProfile = null
            },
            onSave = { name, avatar, colorIdx ->
                val target = editingProfile
                if (target == null) {
                    onCreateChild(name, avatar, colorIdx)
                } else {
                    onUpdateChild(target.id, name, avatar, colorIdx)
                }
                showCreateProfileDialog = false
                editingProfile = null
            }
        )
    }

    deletingProfile?.let { profile ->
        AlertDialog(
            onDismissRequest = { deletingProfile = null },
            title = { Text("Smazat profil?") },
            text = {
                Text("Opravdu chcete smazat profil \"${profile.name}\" včetně jeho hvězdiček a pokroku?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteChild(profile.id)
                        deletingProfile = null
                    },
                    modifier = Modifier.testTag("confirm_delete_child")
                ) {
                    Text("Smazat")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingProfile = null }) {
                    Text("Zrušit")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChildProfileEditorDialog(
    initialProfile: ChildProfile?,
    onDismiss: () -> Unit,
    onSave: (name: String, avatarEmoji: String, colorIndex: Long) -> Unit
) {
    var name by remember { mutableStateOf(initialProfile?.name ?: "") }
    var avatarEmoji by remember { mutableStateOf(initialProfile?.avatarEmoji ?: CHILD_AVATARS.first()) }
    var colorIndex by remember { mutableLongStateOf(initialProfile?.colorIndex ?: 0L) }
    var nameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialProfile == null) "Nový profil dítěte" else "Upravit profil",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it.take(40)
                        nameError = null
                    },
                    label = { Text("Jméno dítěte *") },
                    placeholder = { Text("Např. Anička") },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError?.let { err -> { Text(err) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("child_profile_name_input")
                )

                Text("Obrázek profilu", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CHILD_AVATARS.forEach { candidate ->
                        val isSelected = avatarEmoji == candidate
                        Surface(
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = CircleShape,
                            border = if (isSelected) {
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else null,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .clickable { avatarEmoji = candidate }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(candidate, style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }

                Text("Barva profilu", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PackPalettes.forEachIndexed { idx, palette ->
                        val isSelected = colorIndex.toInt() == idx
                        Surface(
                            color = palette.accent,
                            shape = CircleShape,
                            border = if (isSelected) {
                                BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface)
                            } else null,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable { colorIndex = idx.toLong() }
                        ) {
                            if (isSelected) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = "Zadejte prosím jméno dítěte."
                    } else {
                        onSave(name.trim(), avatarEmoji, colorIndex)
                    }
                },
                modifier = Modifier.testTag("save_child_profile_button")
            ) {
                Text("Uložit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit")
            }
        }
    )
}
