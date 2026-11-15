package com.example.ui.parent

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ChildProfile
import com.example.data.model.ExerciseType
import com.example.data.model.VocabWord
import com.example.data.model.WordPack
import com.example.data.repository.StockPhotoResult
import com.example.ui.ImageGenUiState
import com.example.ui.MainViewModel
import com.example.ui.StockPhotoSearchUiState
import com.example.ui.UiState
import com.example.ui.components.StockPhotoPickerDialog
import com.example.ui.components.WordIllustrationBox
import com.example.ui.components.packPaletteFor
import com.example.ui.theme.PackPalettes

@Composable
fun ParentPacksScreen(
    packs: List<WordPack>,
    childProfiles: List<ChildProfile>,
    wordsState: UiState<List<VocabWord>>,
    imageGenStates: Map<String, ImageGenUiState>,
    stockPhotoSearchState: StockPhotoSearchUiState,
    onSelectPackForEdit: (String?) -> Unit,
    onOpenPack: (WordPack) -> Unit,
    onSavePack: (
        packId: String?,
        title: String,
        description: String,
        emoji: String,
        colorIndex: Long,
        enabledExercises: List<String>,
        assignedChildIds: List<String>
    ) -> Unit,
    onDeletePack: (String) -> Unit,
    onSaveWord: (
        packId: String,
        wordId: String?,
        englishWord: String,
        czechTranslation: String,
        imageBase64: String
    ) -> Unit,
    onDeleteWord: (packId: String, wordId: String) -> Unit,
    onSearchStockPhotos: (String) -> Unit,
    onSelectWordStockPhoto: (VocabWord, StockPhotoResult) -> Unit,
    onSelectDraftStockPhoto: (StockPhotoResult) -> Unit,
    onSelectCustomWordImage: (VocabWord, Uri) -> Unit,
    onSelectCustomDraftImage: (Uri) -> Unit,
    onResetStockPhotoSearch: () -> Unit,
    onResetDraftAi: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingPack by remember { mutableStateOf<WordPack?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var deletingPack by remember { mutableStateOf<WordPack?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("parent_create_pack_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Vytvořit balíček")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .widthIn(max = 880.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 88.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Balíčky slovíček (${packs.size})",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Vytvářejte vlastní témata, přidávejte slovíčka s obrázky a nastavte druhy procvičování.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (packs.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
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
                                Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Zatím nemáte žádné balíčky",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Klikněte na tlačítko + a vytvořte první balíček slovíček.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(packs, key = { it.id }) { pack ->
                    ParentPackRowCard(
                        pack = pack,
                        childProfiles = childProfiles,
                        onOpen = { onOpenPack(pack) },
                        onEdit = {
                            onSelectPackForEdit(pack.id)
                            editingPack = pack
                        },
                        onDelete = { deletingPack = pack }
                    )
                }
            }
        }
    }

    if (showCreateDialog || editingPack != null) {
        PackEditorDialog(
            initialPack = editingPack,
            childProfiles = childProfiles,
            wordsState = wordsState,
            imageGenStates = imageGenStates,
            stockPhotoSearchState = stockPhotoSearchState,
            onSaveWord = { wordId, en, cz, imgBase64 ->
                editingPack?.let { target ->
                    onSaveWord(target.id, wordId, en, cz, imgBase64)
                }
            },
            onDeleteWord = { wordId ->
                editingPack?.let { target ->
                    onDeleteWord(target.id, wordId)
                }
            },
            onSearchStockPhotos = onSearchStockPhotos,
            onSelectWordStockPhoto = onSelectWordStockPhoto,
            onSelectDraftStockPhoto = onSelectDraftStockPhoto,
            onSelectCustomWordImage = onSelectCustomWordImage,
            onSelectCustomDraftImage = onSelectCustomDraftImage,
            onResetStockPhotoSearch = onResetStockPhotoSearch,
            onResetDraftAi = onResetDraftAi,
            onDismiss = {
                showCreateDialog = false
                editingPack = null
                onSelectPackForEdit(null)
            },
            onSave = { title, desc, emoji, colorIdx, exercises, assignedChildIds ->
                onSavePack(
                    editingPack?.id,
                    title,
                    desc,
                    emoji,
                    colorIdx,
                    exercises,
                    assignedChildIds
                )
                showCreateDialog = false
                editingPack = null
                onSelectPackForEdit(null)
            }
        )
    }

    deletingPack?.let { pack ->
        AlertDialog(
            onDismissRequest = { deletingPack = null },
            title = { Text("Smazat balíček?") },
            text = {
                Text("Opravdu chcete smazat balíček \"${pack.title}\" včetně všech jeho slovíček?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePack(pack.id)
                        deletingPack = null
                    },
                    modifier = Modifier.testTag("confirm_delete_pack")
                ) {
                    Text("Smazat")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingPack = null }) {
                    Text("Zrušit")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParentPackRowCard(
    pack: WordPack,
    childProfiles: List<ChildProfile>,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = packPaletteFor(pack.colorIndex)
    val profilesById = childProfiles.associateBy { it.id }
    val assignedLabel = if (pack.assignedChildIds.isEmpty()) {
        "Pro všechny děti"
    } else {
        pack.assignedChildIds.mapNotNull { profilesById[it] }
            .joinToString(", ") { "${it.avatarEmoji} ${it.name}" }
            .ifBlank { "Pro všechny děti" }
    }
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onOpen() }
            .testTag("parent_pack_card_${pack.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pack.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${pack.wordCount} slovíček • $assignedLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("edit_pack_${pack.id}")
                ) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = "Upravit balíček",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("delete_pack_${pack.id}")
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Smazat balíček",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (pack.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = pack.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                pack.parsedExercises().forEach { ex ->
                    Surface(
                        color = palette.container,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = ex.shortTitleCz,
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.onContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PackEditorDialog(
    initialPack: WordPack?,
    childProfiles: List<ChildProfile>,
    wordsState: UiState<List<VocabWord>>,
    imageGenStates: Map<String, ImageGenUiState>,
    stockPhotoSearchState: StockPhotoSearchUiState,
    onSaveWord: (wordId: String?, englishWord: String, czechTranslation: String, imageBase64: String) -> Unit,
    onDeleteWord: (wordId: String) -> Unit,
    onSearchStockPhotos: (String) -> Unit,
    onSelectWordStockPhoto: (VocabWord, StockPhotoResult) -> Unit,
    onSelectDraftStockPhoto: (StockPhotoResult) -> Unit,
    onSelectCustomWordImage: (VocabWord, Uri) -> Unit,
    onSelectCustomDraftImage: (Uri) -> Unit,
    onResetStockPhotoSearch: () -> Unit,
    onResetDraftAi: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        emoji: String,
        colorIndex: Long,
        enabledExercises: List<String>,
        assignedChildIds: List<String>
    ) -> Unit
) {
    var title by remember { mutableStateOf(initialPack?.title ?: "") }
    var description by remember { mutableStateOf(initialPack?.description ?: "") }
    val emoji = initialPack?.emoji ?: "📚"
    var colorIndex by remember { mutableLongStateOf(initialPack?.colorIndex ?: 0L) }
    var selectedExercises by remember {
        mutableStateOf(
            initialPack?.enabledExercises?.toSet() ?: ExerciseType.defaultIds.toSet()
        )
    }
    var selectedChildIds by remember {
        mutableStateOf(initialPack?.assignedChildIds?.toSet() ?: emptySet())
    }
    var titleError by remember { mutableStateOf<String?>(null) }

    var wordFilterQuery by remember { mutableStateOf("") }
    var showWordDialog by remember { mutableStateOf(false) }
    var editingWord by remember { mutableStateOf<VocabWord?>(null) }
    var deletingWord by remember { mutableStateOf<VocabWord?>(null) }
    var stockPhotoTargetWord by remember { mutableStateOf<VocabWord?>(null) }
    var customPhotoTargetWord by remember { mutableStateOf<VocabWord?>(null) }

    val customWordPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val target = customPhotoTargetWord
        customPhotoTargetWord = null
        if (uri != null && target != null) {
            onSelectCustomWordImage(target, uri)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .widthIn(max = 600.dp),
        title = {
            Text(
                text = if (initialPack == null) "Nový balíček slovíček" else "Upravit balíček",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it.take(60)
                        titleError = null
                    },
                    label = { Text("Název balíčka *") },
                    placeholder = { Text("Např. Dopravní prostředky") },
                    singleLine = true,
                    isError = titleError != null,
                    supportingText = titleError?.let { err -> { Text(err) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pack_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(200) },
                    label = { Text("Krátký popis (volitelné)") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pack_desc_input")
                )

                Text("Barva karty", style = MaterialTheme.typography.labelLarge)
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

                Text("Povolené typy cvičení", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ExerciseType.entries.forEach { ex ->
                        val isOn = ex.id in selectedExercises
                        FilterChip(
                            selected = isOn,
                            onClick = {
                                if (isOn && selectedExercises.size > 1) {
                                    selectedExercises = selectedExercises - ex.id
                                } else if (!isOn) {
                                    selectedExercises = selectedExercises + ex.id
                                }
                            },
                            label = { Text(ex.shortTitleCz) },
                            modifier = Modifier.testTag("pack_exercise_chip_${ex.id.lowercase()}")
                        )
                    }
                }

                if (childProfiles.isNotEmpty()) {
                    Text("Přiřazeno dětem", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedChildIds.isEmpty(),
                            onClick = { selectedChildIds = emptySet() },
                            label = { Text("Všechny děti") },
                            modifier = Modifier.testTag("pack_child_all_chip")
                        )
                        childProfiles.forEach { profile ->
                            val isSelected = profile.id in selectedChildIds
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedChildIds = if (isSelected) {
                                        selectedChildIds - profile.id
                                    } else {
                                        selectedChildIds + profile.id
                                    }
                                },
                                label = { Text("${profile.avatarEmoji} ${profile.name}") },
                                modifier = Modifier.testTag("pack_child_chip_${profile.id}")
                            )
                        }
                    }
                }

                if (initialPack != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    val words = (wordsState as? UiState.Success)?.data.orEmpty()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Slovíčka v balíčku (${words.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        FilledTonalButton(
                            onClick = {
                                editingWord = null
                                onResetDraftAi()
                                showWordDialog = true
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("pack_editor_add_word")
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Přidat")
                        }
                    }

                    when (wordsState) {
                        is UiState.Loading -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                        is UiState.Error -> {
                            Text(
                                text = wordsState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        is UiState.Success -> {
                            if (words.isEmpty()) {
                                Text(
                                    text = "Balíček zatím neobsahuje žádná slovíčka. Přidejte první tlačítkem Přidat.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                OutlinedTextField(
                                    value = wordFilterQuery,
                                    onValueChange = { wordFilterQuery = it.take(40) },
                                    placeholder = { Text("Vyhledat slovíčko v balíčku...") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Rounded.Search,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("pack_editor_word_filter")
                                )
                                val cleanFilter = wordFilterQuery.trim().lowercase()
                                val filteredWords = if (cleanFilter.isBlank()) {
                                    words
                                } else {
                                    words.filter {
                                        it.englishWord.lowercase().contains(cleanFilter) ||
                                            it.czechTranslation.lowercase().contains(cleanFilter)
                                    }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    filteredWords.forEach { word ->
                                        val wordImgState = imageGenStates[word.id] ?: ImageGenUiState.Idle
                                        val isGenerating = wordImgState is ImageGenUiState.Loading
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("pack_editor_word_row_${word.id}")
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    WordIllustrationBox(
                                                        cacheKey = word.id,
                                                        imageBase64 = word.imageBase64,
                                                        fallbackEmoji = emoji,
                                                        englishWord = word.englishWord,
                                                        contentDescription = word.czechTranslation,
                                                        isLoading = isGenerating,
                                                        modifier = Modifier
                                                            .size(48.dp)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .clickable {
                                                                onResetStockPhotoSearch()
                                                                stockPhotoTargetWord = word
                                                            }
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = word.englishWord,
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = word.czechTranslation,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            editingWord = word
                                                            onResetDraftAi()
                                                            showWordDialog = true
                                                        },
                                                        modifier = Modifier
                                                            .size(48.dp)
                                                            .testTag("pack_editor_edit_word_${word.id}")
                                                    ) {
                                                        Icon(
                                                            Icons.Rounded.Edit,
                                                            contentDescription = "Upravit slovíčko",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { deletingWord = word },
                                                        modifier = Modifier
                                                            .size(48.dp)
                                                            .testTag("pack_editor_delete_word_${word.id}")
                                                    ) {
                                                        Icon(
                                                            Icons.Rounded.Delete,
                                                            contentDescription = "Smazat slovíčko",
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    FilledTonalButton(
                                                        onClick = {
                                                            onResetStockPhotoSearch()
                                                            stockPhotoTargetWord = word
                                                        },
                                                        enabled = !isGenerating,
                                                        shape = RoundedCornerShape(12.dp),
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(48.dp)
                                                            .testTag("pack_editor_photo_${word.id}")
                                                    ) {
                                                        Icon(
                                                            Icons.Rounded.PhotoLibrary,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "Databanka",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    }
                                                    FilledTonalButton(
                                                        onClick = {
                                                            customPhotoTargetWord = word
                                                            customWordPhotoLauncher.launch(
                                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                            )
                                                        },
                                                        enabled = !isGenerating,
                                                        shape = RoundedCornerShape(12.dp),
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(48.dp)
                                                            .testTag("pack_editor_upload_${word.id}")
                                                    ) {
                                                        Icon(
                                                            Icons.Rounded.AddPhotoAlternate,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "Vlastní",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    }
                                                }

                                                if (wordImgState is ImageGenUiState.Error) {
                                                    Text(
                                                        text = wordImgState.message,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Po vytvoření balíčka můžete v jeho nastavení přidávat slovíčka a vybírat obrázky.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = "Zadejte prosím název balíčka."
                    } else {
                        onSave(
                            title.trim(),
                            description.trim(),
                            emoji,
                            colorIndex,
                            selectedExercises.toList(),
                            selectedChildIds.toList()
                        )
                    }
                },
                modifier = Modifier.testTag("save_pack_button")
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

    stockPhotoTargetWord?.let { targetWord ->
        StockPhotoPickerDialog(
            initialQuery = targetWord.englishWord,
            searchState = stockPhotoSearchState,
            onSearch = onSearchStockPhotos,
            onSelectPhoto = { photo ->
                onSelectWordStockPhoto(targetWord, photo)
                stockPhotoTargetWord = null
            },
            onDismiss = { stockPhotoTargetWord = null }
        )
    }

    if (showWordDialog) {
        val draftImgState = imageGenStates[MainViewModel.DRAFT_KEY] ?: ImageGenUiState.Idle
        WordEditorDialog(
            initialWord = editingWord,
            fallbackEmoji = emoji,
            draftImageState = draftImgState,
            stockPhotoSearchState = stockPhotoSearchState,
            onSearchStockPhotos = onSearchStockPhotos,
            onSelectDraftStockPhoto = onSelectDraftStockPhoto,
            onSelectCustomImage = onSelectCustomDraftImage,
            onResetStockPhotoSearch = onResetStockPhotoSearch,
            onDismiss = {
                showWordDialog = false
                editingWord = null
            },
            onSave = { en, cz, imgBase64 ->
                onSaveWord(editingWord?.id, en, cz, imgBase64)
                showWordDialog = false
                editingWord = null
            }
        )
    }

    deletingWord?.let { word ->
        AlertDialog(
            onDismissRequest = { deletingWord = null },
            title = { Text("Smazat slovíčko?") },
            text = { Text("Opravdu chcete smazat slovíčko \"${word.englishWord}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteWord(word.id)
                        deletingWord = null
                    },
                    modifier = Modifier.testTag("confirm_delete_word_in_pack_editor")
                ) {
                    Text("Smazat")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingWord = null }) {
                    Text("Zrušit")
                }
            }
        )
    }
}
