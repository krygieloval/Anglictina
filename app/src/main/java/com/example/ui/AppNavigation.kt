package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ExerciseType
import com.example.data.model.VocabWord
import com.example.ui.child.AnagramExerciseScreen
import com.example.ui.child.ChildHomeScreen
import com.example.ui.child.ChildPackHubScreen
import com.example.ui.child.FillBlankExerciseScreen
import com.example.ui.child.MatchingExerciseScreen
import com.example.ui.child.MemoryGameScreen
import com.example.ui.child.PracticeSummaryScreen
import com.example.ui.child.PronunciationExerciseScreen
import com.example.ui.child.QuizExerciseScreen
import com.example.ui.child.WordShooterGameScreen
import com.example.ui.components.ChildProfileSwitcherDialog
import com.example.ui.components.ParentalGateDialog
import com.example.ui.components.SlovickaTopHeader
import com.example.ui.parent.ParentPackDetailScreen
import com.example.ui.parent.ParentPacksScreen
import com.example.ui.parent.ParentProgressScreen
import com.example.ui.parent.ParentSettingsScreen
import com.example.util.VocabTtsPlayer

private sealed interface ChildFlowScreen {
    data object Home : ChildFlowScreen
    data class PackHub(val packId: String) : ChildFlowScreen
    data class Exercise(
        val packId: String,
        val type: ExerciseType,
        val words: List<VocabWord>,
        val attemptSeed: Int = 0
    ) : ChildFlowScreen

    data class Summary(
        val packId: String,
        val type: ExerciseType,
        val words: List<VocabWord>,
        val outcomes: Map<VocabWord, Boolean>,
        val starsEarned: Int,
        val score: Int,
        val movesCount: Int? = null
    ) : ChildFlowScreen
}

private enum class ParentTab(val label: String) {
    PACKS("Balíčky"),
    PROGRESS("Pokrok"),
    SETTINGS("Nastavení")
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModel.provideFactory(context)
    )

    val ttsPlayer = remember(context) { VocabTtsPlayer(context) }
    DisposableEffect(ttsPlayer) {
        onDispose { ttsPlayer.release() }
    }

    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val wordsState by viewModel.selectedPackWordsState.collectAsStateWithLifecycle()
    val imageGenStates by viewModel.imageGenStates.collectAsStateWithLifecycle()
    val stockPhotoSearchState by viewModel.stockPhotoSearchState.collectAsStateWithLifecycle()
    val isBulkFillingPhotos by viewModel.isBulkFillingPhotos.collectAsStateWithLifecycle()
    val appMode by viewModel.appMode.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(actionMessage) {
        actionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionMessage()
        }
    }

    var showParentGate by remember { mutableStateOf(false) }
    var showProfileSwitcher by remember { mutableStateOf(false) }
    var childScreen by remember { mutableStateOf<ChildFlowScreen>(ChildFlowScreen.Home) }
    var parentTab by remember { mutableStateOf(ParentTab.PACKS) }
    var parentOpenedPackId by remember { mutableStateOf<String?>(null) }
    var exerciseSeed by remember { mutableIntStateOf(0) }

    val isExpandedWidth = LocalConfiguration.current.screenWidthDp >= 720

    BackHandler(
        enabled = (appMode == AppMode.CHILD && childScreen !is ChildFlowScreen.Home) ||
            (appMode == AppMode.PARENT)
    ) {
        if (appMode == AppMode.CHILD) {
            when (val screen = childScreen) {
                is ChildFlowScreen.Home -> Unit
                is ChildFlowScreen.PackHub -> {
                    viewModel.selectPack(null)
                    childScreen = ChildFlowScreen.Home
                }
                is ChildFlowScreen.Exercise -> {
                    childScreen = ChildFlowScreen.PackHub(screen.packId)
                }
                is ChildFlowScreen.Summary -> {
                    childScreen = ChildFlowScreen.PackHub(screen.packId)
                }
            }
        } else {
            if (parentOpenedPackId != null) {
                parentOpenedPackId = null
                viewModel.selectPack(null)
            } else {
                viewModel.selectPack(null)
                viewModel.setAppMode(AppMode.CHILD)
                childScreen = ChildFlowScreen.Home
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (appMode == AppMode.PARENT && parentOpenedPackId == null && !isExpandedWidth) {
                NavigationBar {
                    ParentTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = parentTab == tab,
                            onClick = { parentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = when (tab) {
                                        ParentTab.PACKS -> Icons.Rounded.Folder
                                        ParentTab.PROGRESS -> Icons.Rounded.BarChart
                                        ParentTab.SETTINGS -> Icons.Rounded.Settings
                                    },
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) },
                            modifier = Modifier.testTag("parent_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = dashboardState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Načítám slovíčka...", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                is UiState.Success -> {
                    val dashboard = state.data
                    val showTopHeader = appMode == AppMode.PARENT || childScreen is ChildFlowScreen.Home

                    if (showTopHeader) {
                        SlovickaTopHeader(
                            activeChild = dashboard.activeChild,
                            currentMode = appMode,
                            onOpenProfileSwitcher = { showProfileSwitcher = true },
                            onSwitchModeRequested = {
                                if (appMode == AppMode.CHILD) {
                                    showParentGate = true
                                } else {
                                    parentOpenedPackId = null
                                    viewModel.selectPack(null)
                                    viewModel.setAppMode(AppMode.CHILD)
                                    childScreen = ChildFlowScreen.Home
                                }
                            }
                        )
                    }

                    if (appMode == AppMode.CHILD) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            when (val screen = childScreen) {
                                is ChildFlowScreen.Home -> {
                                    ChildHomeScreen(
                                        dashboard = dashboard,
                                        onOpenPack = { pack ->
                                            viewModel.selectPack(pack.id)
                                            childScreen = ChildFlowScreen.PackHub(pack.id)
                                        }
                                    )
                                }
                                is ChildFlowScreen.PackHub -> {
                                    val pack = dashboard.packs.firstOrNull { it.id == screen.packId }
                                    if (pack == null) {
                                        childScreen = ChildFlowScreen.Home
                                    } else {
                                        ChildPackHubScreen(
                                            pack = pack,
                                            wordsState = wordsState,
                                            onBack = {
                                                viewModel.selectPack(null)
                                                childScreen = ChildFlowScreen.Home
                                            },
                                            onSpeakWord = { ttsPlayer.speakEnglish(it) },
                                            onStartExercise = { type, words ->
                                                exerciseSeed += 1
                                                childScreen = ChildFlowScreen.Exercise(
                                                    packId = pack.id,
                                                    type = type,
                                                    words = words,
                                                    attemptSeed = exerciseSeed
                                                )
                                            }
                                        )
                                    }
                                }
                                is ChildFlowScreen.Exercise -> {
                                    val pack = dashboard.packs.firstOrNull { it.id == screen.packId }
                                    val emoji = pack?.emoji ?: "📚"
                                    val finishHandler: (Map<VocabWord, Boolean>, Int, Int) -> Unit =
                                        { outcomes, stars, score ->
                                            viewModel.recordPracticeSession(
                                                exerciseType = screen.type,
                                                wordOutcomes = outcomes,
                                                starsEarned = stars,
                                                score = score
                                            )
                                            childScreen = ChildFlowScreen.Summary(
                                                packId = screen.packId,
                                                type = screen.type,
                                                words = screen.words,
                                                outcomes = outcomes,
                                                starsEarned = stars,
                                                score = score
                                            )
                                        }

                                    androidx.compose.runtime.key(screen.attemptSeed) {
                                        when (screen.type) {
                                            ExerciseType.MATCHING -> MatchingExerciseScreen(
                                                words = screen.words,
                                                fallbackEmoji = emoji,
                                                onSpeakWord = { ttsPlayer.speakEnglish(it) },
                                                onSpeakCzech = { ttsPlayer.speakCzech(it) },
                                                onBack = { childScreen = ChildFlowScreen.PackHub(screen.packId) },
                                                onFinish = finishHandler
                                            )
                                            ExerciseType.FILL_BLANK -> FillBlankExerciseScreen(
                                                words = screen.words,
                                                fallbackEmoji = emoji,
                                                onSpeakWord = { ttsPlayer.speakEnglish(it) },
                                                onSpeakCzech = { ttsPlayer.speakCzech(it) },
                                                onBack = { childScreen = ChildFlowScreen.PackHub(screen.packId) },
                                                onFinish = finishHandler
                                            )
                                            ExerciseType.PRONUNCIATION -> PronunciationExerciseScreen(
                                                words = screen.words,
                                                fallbackEmoji = emoji,
                                                onSpeakWord = { ttsPlayer.speakEnglish(it) },
                                                onBack = { childScreen = ChildFlowScreen.PackHub(screen.packId) },
                                                onFinish = finishHandler
                                            )
                                            ExerciseType.SHOOTER -> WordShooterGameScreen(
                                                words = screen.words,
                                                fallbackEmoji = emoji,
                                                onSpeakWord = { ttsPlayer.speakEnglish(it) },
                                                onSpeakCzech = { ttsPlayer.speakCzech(it) },
                                                onBack = { childScreen = ChildFlowScreen.PackHub(screen.packId) },
                                                onFinish = finishHandler
                                            )
                                            ExerciseType.MEMORY -> MemoryGameScreen(
                                                words = screen.words,
                                                fallbackEmoji = emoji,
                                                onSpeakWord = { ttsPlayer.speakEnglish(it) },
                                                onSpeakCzech = { ttsPlayer.speakCzech(it) },
                                                onBack = { childScreen = ChildFlowScreen.PackHub(screen.packId) },
                                                onFinish = { outcomes, stars, score, moves ->
                                                    viewModel.recordPracticeSession(
                                                        exerciseType = screen.type,
                                                        wordOutcomes = outcomes,
                                                        starsEarned = stars,
                                                        score = score
                                                    )
                                                    childScreen = ChildFlowScreen.Summary(
                                                        packId = screen.packId,
                                                        type = screen.type,
                                                        words = screen.words,
                                                        outcomes = outcomes,
                                                        starsEarned = stars,
                                                        score = score,
                                                        movesCount = moves
                                                    )
                                                }
                                            )
                                            ExerciseType.ANAGRAM -> AnagramExerciseScreen(
                                                words = screen.words,
                                                fallbackEmoji = emoji,
                                                onSpeakWord = { ttsPlayer.speakEnglish(it) },
                                                onBack = { childScreen = ChildFlowScreen.PackHub(screen.packId) },
                                                onFinish = finishHandler
                                            )
                                            ExerciseType.QUIZ -> QuizExerciseScreen(
                                                words = screen.words,
                                                fallbackEmoji = emoji,
                                                onSpeakWord = { ttsPlayer.speakEnglish(it) },
                                                onBack = { childScreen = ChildFlowScreen.PackHub(screen.packId) },
                                                onFinish = finishHandler
                                            )
                                        }
                                    }
                                }
                                is ChildFlowScreen.Summary -> {
                                    PracticeSummaryScreen(
                                        exerciseType = screen.type,
                                        wordOutcomes = screen.outcomes,
                                        starsEarned = screen.starsEarned,
                                        score = screen.score,
                                        movesCount = screen.movesCount,
                                        onPlayAgain = {
                                            exerciseSeed += 1
                                            childScreen = ChildFlowScreen.Exercise(
                                                packId = screen.packId,
                                                type = screen.type,
                                                words = screen.words,
                                                attemptSeed = exerciseSeed
                                            )
                                        },
                                        onDone = {
                                            childScreen = ChildFlowScreen.PackHub(screen.packId)
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // Parent Mode
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                        ) {
                            if (isExpandedWidth && parentOpenedPackId == null) {
                                NavigationRail {
                                    ParentTab.entries.forEach { tab ->
                                        NavigationRailItem(
                                            selected = parentTab == tab,
                                            onClick = { parentTab = tab },
                                            icon = {
                                                Icon(
                                                    imageVector = when (tab) {
                                                        ParentTab.PACKS -> Icons.Rounded.Folder
                                                        ParentTab.PROGRESS -> Icons.Rounded.BarChart
                                                        ParentTab.SETTINGS -> Icons.Rounded.Settings
                                                    },
                                                    contentDescription = tab.label
                                                )
                                            },
                                            label = { Text(tab.label) }
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                val openedPack = parentOpenedPackId?.let { id ->
                                    dashboard.allPacks.firstOrNull { it.id == id }
                                }
                                if (openedPack != null) {
                                    ParentPackDetailScreen(
                                        pack = openedPack,
                                        childProfiles = dashboard.childProfiles,
                                        wordsState = wordsState,
                                        imageGenStates = imageGenStates,
                                        stockPhotoSearchState = stockPhotoSearchState,
                                        isBulkFillingPhotos = isBulkFillingPhotos,
                                        onBack = {
                                            parentOpenedPackId = null
                                            viewModel.selectPack(null)
                                        },
                                        onSpeakWord = { ttsPlayer.speakEnglish(it) },
                                        onToggleExercise = { exType ->
                                            viewModel.togglePackExercise(openedPack, exType)
                                        },
                                        onUpdateAssignedChildren = { assignedIds ->
                                            viewModel.updatePackChildAssignments(openedPack, assignedIds)
                                        },
                                        onSaveWord = { wordId, en, cz, imgBase64 ->
                                            viewModel.saveWord(
                                                packId = openedPack.id,
                                                wordId = wordId,
                                                englishWord = en,
                                                czechTranslation = cz,
                                                imageBase64 = imgBase64
                                            )
                                        },
                                        onDeleteWord = { wordId ->
                                            viewModel.deleteWord(openedPack.id, wordId)
                                        },
                                        onSearchStockPhotos = { query ->
                                            viewModel.searchStockPhotos(query)
                                        },
                                        onSelectWordStockPhoto = { word, photo ->
                                            viewModel.selectStockPhotoForWord(word, photo)
                                        },
                                        onSelectDraftStockPhoto = { photo ->
                                            viewModel.selectStockPhotoForDraft(photo)
                                        },
                                        onSelectCustomWordImage = { word, uri ->
                                            viewModel.selectCustomImageForWord(context, word, uri)
                                        },
                                        onSelectCustomDraftImage = { uri ->
                                            viewModel.selectCustomImageForDraft(context, uri)
                                        },
                                        onFillPackWithStockPhotos = { words ->
                                            viewModel.fillPackWithStockPhotos(words)
                                        },
                                        onResetStockPhotoSearch = {
                                            viewModel.resetStockPhotoSearch()
                                        },
                                        onResetDraftAi = { viewModel.resetDraftAiStates() }
                                    )
                                } else {
                                    when (parentTab) {
                                        ParentTab.PACKS -> ParentPacksScreen(
                                            packs = dashboard.allPacks,
                                            childProfiles = dashboard.childProfiles,
                                            wordsState = wordsState,
                                            imageGenStates = imageGenStates,
                                            stockPhotoSearchState = stockPhotoSearchState,
                                            onSelectPackForEdit = { packId ->
                                                viewModel.selectPack(packId)
                                            },
                                            onOpenPack = { pack ->
                                                viewModel.selectPack(pack.id)
                                                parentOpenedPackId = pack.id
                                            },
                                            onSavePack = { id, title, desc, emoji, colorIdx, exercises, assignedChildIds ->
                                                viewModel.savePack(
                                                    packId = id,
                                                    title = title,
                                                    description = desc,
                                                    emoji = emoji,
                                                    colorIndex = colorIdx,
                                                    enabledExercises = exercises,
                                                    assignedChildIds = assignedChildIds
                                                )
                                            },
                                            onDeletePack = { packId ->
                                                viewModel.deletePack(packId)
                                            },
                                            onSaveWord = { packId, wordId, en, cz, imgBase64 ->
                                                viewModel.saveWord(
                                                    packId = packId,
                                                    wordId = wordId,
                                                    englishWord = en,
                                                    czechTranslation = cz,
                                                    imageBase64 = imgBase64
                                                )
                                            },
                                            onDeleteWord = { packId, wordId ->
                                                viewModel.deleteWord(packId, wordId)
                                            },
                                            onSearchStockPhotos = { query ->
                                                viewModel.searchStockPhotos(query)
                                            },
                                            onSelectWordStockPhoto = { word, photo ->
                                                viewModel.selectStockPhotoForWord(word, photo)
                                            },
                                            onSelectDraftStockPhoto = { photo ->
                                                viewModel.selectStockPhotoForDraft(photo)
                                            },
                                            onSelectCustomWordImage = { word, uri ->
                                                viewModel.selectCustomImageForWord(context, word, uri)
                                            },
                                            onSelectCustomDraftImage = { uri ->
                                                viewModel.selectCustomImageForDraft(context, uri)
                                            },
                                            onResetStockPhotoSearch = {
                                                viewModel.resetStockPhotoSearch()
                                            },
                                            onResetDraftAi = {
                                                viewModel.resetDraftAiStates()
                                            }
                                        )
                                        ParentTab.PROGRESS -> ParentProgressScreen(
                                            dashboard = dashboard,
                                            onSelectChild = { childId ->
                                                viewModel.switchChildProfile(childId)
                                            }
                                        )
                                        ParentTab.SETTINGS -> ParentSettingsScreen(
                                            settings = dashboard.settings,
                                            childProfiles = dashboard.childProfiles,
                                            activeChildId = dashboard.activeChild.id,
                                            onSwitchChild = { childId ->
                                                viewModel.switchChildProfile(childId)
                                            },
                                            onCreateChild = { name, avatar, colorIdx ->
                                                viewModel.createChildProfile(name, avatar, colorIdx)
                                            },
                                            onUpdateChild = { childId, name, avatar, colorIdx ->
                                                viewModel.updateChildProfile(childId, name, avatar, colorIdx)
                                            },
                                            onDeleteChild = { childId ->
                                                viewModel.deleteChildProfile(childId)
                                            },
                                            onSaveSettings = { pinOn, pin ->
                                                viewModel.saveParentSettings(pinOn, pin)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (showProfileSwitcher) {
                        ChildProfileSwitcherDialog(
                            profiles = dashboard.childProfiles,
                            activeChildId = dashboard.activeChild.id,
                            onSelectProfile = { profile ->
                                viewModel.switchChildProfile(profile.id)
                                showProfileSwitcher = false
                            },
                            onDismiss = { showProfileSwitcher = false }
                        )
                    }

                    if (showParentGate) {
                        ParentalGateDialog(
                            settings = dashboard.settings,
                            onUnlocked = {
                                showParentGate = false
                                parentOpenedPackId = null
                                viewModel.selectPack(null)
                                viewModel.setAppMode(AppMode.PARENT)
                            },
                            onDismiss = { showParentGate = false }
                        )
                    }
                }
            }
        }
    }
}
