package com.example.ui.parent

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ChildProfile
import com.example.data.model.ExerciseType
import com.example.data.model.VocabWord
import com.example.data.model.WordPack
import com.example.data.repository.StockPhotoResult
import com.example.ui.ImageGenUiState
import com.example.ui.MainViewModel
import com.example.ui.StockPhotoSearchUiState
import com.example.ui.UiState
import com.example.ui.components.SpeakButton
import com.example.ui.components.StockPhotoPickerDialog
import com.example.ui.components.WordIllustrationBox
import com.example.ui.components.packPaletteFor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParentPackDetailScreen(
    pack: WordPack,
    childProfiles: List<ChildProfile>,
    wordsState: UiState<List<VocabWord>>,
    imageGenStates: Map<String, ImageGenUiState>,
    stockPhotoSearchState: StockPhotoSearchUiState,
    isBulkFillingPhotos: Boolean,
    onBack: () -> Unit,
    onSpeakWord: (String) -> Unit,
    onToggleExercise: (ExerciseType) -> Unit,
    onUpdateAssignedChildren: (List<String>) -> Unit,
    onSaveWord: (
        wordId: String?,
        englishWord: String,
        czechTranslation: String,
        imageBase64: String
    ) -> Unit,
    onDeleteWord: (String) -> Unit,
    onSearchStockPhotos: (String) -> Unit,
    onSelectWordStockPhoto: (VocabWord, StockPhotoResult) -> Unit,
    onSelectDraftStockPhoto: (StockPhotoResult) -> Unit,
    onSelectCustomWordImage: (VocabWord, Uri) -> Unit,
    onSelectCustomDraftImage: (Uri) -> Unit,
    onFillPackWithStockPhotos: (List<VocabWord>) -> Unit,
    onResetStockPhotoSearch: () -> Unit,
    onResetDraftAi: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var showWordDialog by remember { mutableStateOf(false) }
    var editingWord by remember { mutableStateOf<VocabWord?>(null) }
    var deletingWord by remember { mutableStateOf<VocabWord?>(null) }
    var stockPhotoTargetWord by remember { mutableStateOf<VocabWord?>(null) }
    var customPhotoTargetWord by remember { mutableStateOf<VocabWord?>(null) }
    val palette = packPaletteFor(pack.colorIndex)

    val customPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        val target = customPhotoTargetWord
        customPhotoTargetWord = null
        if (uri != null && target != null) {
            onSelectCustomWordImage(target, uri)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingWord = null
                    onResetDraftAi()
                    showWordDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("parent_add_word_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Přidat slovíčko")
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
                Surface(
                    color = palette.container,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("parent_pack_detail_back")
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = "Zpět na balíčky",
                                    tint = palette.onContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pack.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = palette.onContainer
                                )
                                Text(
                                    text = "${pack.wordCount} slovíček",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.onContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Povolené typy cvičení v tomto balíčku:",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.onContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ExerciseType.entries.forEach { ex ->
                                val isOn = ex.id in pack.enabledExercises
                                FilterChip(
                                    selected = isOn,
                                    onClick = { onToggleExercise(ex) },
                                    label = { Text(ex.shortTitleCz) },
                                    modifier = Modifier.testTag("detail_toggle_${ex.id.lowercase()}")
                                )
                            }
                        }

                        if (childProfiles.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Přiřazeno dětem:",
                                style = MaterialTheme.typography.labelLarge,
                                color = palette.onContainer
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = pack.assignedChildIds.isEmpty(),
                                    onClick = { onUpdateAssignedChildren(emptyList()) },
                                    label = { Text("Všechny děti") },
                                    modifier = Modifier.testTag("detail_child_all")
                                )
                                childProfiles.forEach { profile ->
                                    val isAssigned = profile.id in pack.assignedChildIds
                                    FilterChip(
                                        selected = isAssigned,
                                        onClick = {
                                            val updated = if (isAssigned) {
                                                pack.assignedChildIds - profile.id
                                            } else {
                                                pack.assignedChildIds + profile.id
                                            }
                                            onUpdateAssignedChildren(updated)
                                        },
                                        label = { Text("${profile.avatarEmoji} ${profile.name}") },
                                        modifier = Modifier.testTag("detail_child_${profile.id}")
                                    )
                                }
                            }
                        }

                        if (wordsState is UiState.Success && wordsState.data.any { it.imageBase64.isBlank() }) {
                            Spacer(modifier = Modifier.height(10.dp))
                            FilledTonalButton(
                                onClick = { onFillPackWithStockPhotos(wordsState.data) },
                                enabled = !isBulkFillingPhotos,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("bulk_fill_stock_photos_button")
                            ) {
                                if (isBulkFillingPhotos) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Stahuji fotky z fotobanky...")
                                } else {
                                    Icon(
                                        Icons.Rounded.PhotoLibrary,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Doplnit fotky z fotobanky pro balíček")
                                }
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
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(16.dp),
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
                    if (words.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Balíček je zatím prázdný. Přidejte první slovíčko tlačítkem + vpravo dole!",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    } else {
                        items(words, key = { it.id }) { word ->
                            val imgState = imageGenStates[word.id] ?: ImageGenUiState.Idle
                            ParentWordCard(
                                word = word,
                                fallbackEmoji = pack.emoji,
                                imageGenState = imgState,
                                onSpeak = { onSpeakWord(word.englishWord) },
                                onOpenStockPhotoPicker = {
                                    onResetStockPhotoSearch()
                                    stockPhotoTargetWord = word
                                },
                                onPickCustomImage = {
                                    customPhotoTargetWord = word
                                    customPhotoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onEdit = {
                                    editingWord = word
                                    onResetDraftAi()
                                    showWordDialog = true
                                },
                                onDelete = { deletingWord = word }
                            )
                        }
                    }
                }
            }
        }
    }

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
            fallbackEmoji = pack.emoji,
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
                    modifier = Modifier.testTag("confirm_delete_word")
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

@Composable
private fun ParentWordCard(
    word: VocabWord,
    fallbackEmoji: String,
    imageGenState: ImageGenUiState,
    onSpeak: () -> Unit,
    onOpenStockPhotoPicker: () -> Unit,
    onPickCustomImage: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isGenerating = imageGenState is ImageGenUiState.Loading
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("parent_word_card_${word.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WordIllustrationBox(
                    cacheKey = word.id,
                    imageBase64 = word.imageBase64,
                    fallbackEmoji = fallbackEmoji,
                    englishWord = word.englishWord,
                    contentDescription = word.czechTranslation,
                    isLoading = isGenerating,
                    modifier = Modifier.size(76.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = word.englishWord,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = word.czechTranslation,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("edit_word_${word.id}")
                        ) {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = "Upravit slovíčko",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("delete_word_${word.id}")
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Smazat slovíčko",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    SpeakButton(onClick = onSpeak)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onOpenStockPhotoPicker,
                    enabled = !isGenerating,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("stock_photo_btn_${word.id}")
                ) {
                    Icon(
                        Icons.Rounded.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isGenerating) "Načítám..." else "Databanka",
                        maxLines = 1,
                        softWrap = false
                    )
                }
                FilledTonalButton(
                    onClick = onPickCustomImage,
                    enabled = !isGenerating,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("custom_photo_btn_${word.id}")
                ) {
                    Icon(
                        Icons.Rounded.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Vlastní",
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            if (imageGenState is ImageGenUiState.Error) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = imageGenState.message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WordEditorDialog(
    initialWord: VocabWord?,
    fallbackEmoji: String,
    draftImageState: ImageGenUiState,
    stockPhotoSearchState: StockPhotoSearchUiState,
    onSearchStockPhotos: (String) -> Unit,
    onSelectDraftStockPhoto: (StockPhotoResult) -> Unit,
    onSelectCustomImage: (Uri) -> Unit,
    onResetStockPhotoSearch: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (
        englishWord: String,
        czechTranslation: String,
        imageBase64: String
    ) -> Unit
) {
    var englishWord by remember { mutableStateOf(initialWord?.englishWord ?: "") }
    var czechTranslation by remember { mutableStateOf(initialWord?.czechTranslation ?: "") }
    var imageBase64 by remember { mutableStateOf(initialWord?.imageBase64 ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showStockPhotoDialog by remember { mutableStateOf(false) }

    val customImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onSelectCustomImage(uri)
        }
    }

    LaunchedEffect(draftImageState) {
        if (draftImageState is ImageGenUiState.Success) {
            imageBase64 = draftImageState.base64
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialWord == null) "Nové slovíčko" else "Upravit slovíčko",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = englishWord,
                    onValueChange = {
                        englishWord = it.take(60)
                        validationError = null
                    },
                    label = { Text("Anglické slovíčko *") },
                    placeholder = { Text("Např. Butterfly") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("word_english_input")
                )

                OutlinedTextField(
                    value = czechTranslation,
                    onValueChange = {
                        czechTranslation = it.take(60)
                        validationError = null
                    },
                    label = { Text("Český překlad *") },
                    placeholder = { Text("Např. Motýl") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("word_czech_input")
                )

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WordIllustrationBox(
                        cacheKey = initialWord?.id ?: "draft_preview",
                        imageBase64 = imageBase64,
                        fallbackEmoji = fallbackEmoji,
                        englishWord = englishWord,
                        czechTranslation = czechTranslation,
                        contentDescription = "Náhled obrázku",
                        isLoading = draftImageState is ImageGenUiState.Loading,
                        modifier = Modifier.size(84.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                onResetStockPhotoSearch()
                                showStockPhotoDialog = true
                            },
                            enabled = draftImageState !is ImageGenUiState.Loading,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("dialog_stock_photo_button")
                        ) {
                            Icon(
                                Icons.Rounded.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (draftImageState is ImageGenUiState.Loading) {
                                    "Načítám..."
                                } else {
                                    "Vybrat z databanky"
                                }
                            )
                        }
                        FilledTonalButton(
                            onClick = {
                                customImageLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = draftImageState !is ImageGenUiState.Loading,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("dialog_upload_image_button")
                        ) {
                            Icon(
                                Icons.Rounded.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Nahrát vlastní obrázek")
                        }
                        if (imageBase64.isNotBlank()) {
                            TextButton(
                                onClick = { imageBase64 = "" },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Text("Odebrat obrázek")
                            }
                        }
                    }
                }

                if (showStockPhotoDialog) {
                    StockPhotoPickerDialog(
                        initialQuery = englishWord.ifBlank { czechTranslation },
                        searchState = stockPhotoSearchState,
                        onSearch = onSearchStockPhotos,
                        onSelectPhoto = { photo ->
                            onSelectDraftStockPhoto(photo)
                            showStockPhotoDialog = false
                        },
                        onDismiss = { showStockPhotoDialog = false }
                    )
                }

                if (draftImageState is ImageGenUiState.Loading) {
                    Text(
                        text = "Zpracovávám obrázek...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (draftImageState is ImageGenUiState.Error) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = draftImageState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (englishWord.isBlank() || czechTranslation.isBlank()) {
                        validationError = "Vyplňte prosím anglické slovíčko i český překlad."
                    } else {
                        onSave(
                            englishWord.trim(),
                            czechTranslation.trim(),
                            imageBase64
                        )
                    }
                },
                modifier = Modifier.testTag("save_word_button")
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
