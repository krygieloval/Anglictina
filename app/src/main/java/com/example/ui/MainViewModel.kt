package com.example.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.ChildProfile
import com.example.data.model.ExerciseType
import com.example.data.model.ExerciseTypeStats
import com.example.data.model.ParentSettings
import com.example.data.model.VocabWord
import com.example.data.model.WordPack
import com.example.data.model.WordProgress
import com.example.data.repository.StockPhotoRepository
import com.example.data.repository.StockPhotoResult
import com.example.data.repository.VocabRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppMode {
    CHILD,
    PARENT
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface ImageGenUiState {
    data object Idle : ImageGenUiState
    data object Loading : ImageGenUiState
    data class Success(val base64: String) : ImageGenUiState
    data class Error(val message: String) : ImageGenUiState
}

sealed interface StockPhotoSearchUiState {
    data object Idle : StockPhotoSearchUiState
    data object Loading : StockPhotoSearchUiState
    data class Success(val query: String, val results: List<StockPhotoResult>) : StockPhotoSearchUiState
    data class Error(val message: String) : StockPhotoSearchUiState
}

data class PackProgressSummary(
    val pack: WordPack,
    val practicedWordsCount: Int,
    val masteredWordsCount: Int,
    val accuracyPercent: Int
)

data class ExerciseStatSummary(
    val type: ExerciseType,
    val attempts: Long,
    val correctCount: Long,
    val sessionsCompleted: Long,
    val bestScore: Long,
    val accuracyPercent: Int
)

data class ParentAnalytics(
    val totalWordsCount: Int,
    val practicedWordsCount: Int,
    val learnedWordsCount: Int,
    val overallAccuracyPercent: Int,
    val weakWords: List<WordProgress>,
    val masteredWords: List<WordProgress>,
    val packSummaries: List<PackProgressSummary>,
    val exerciseBreakdown: List<ExerciseStatSummary>
)

data class DashboardData(
    val packs: List<WordPack>,
    val allPacks: List<WordPack>,
    val childProfiles: List<ChildProfile>,
    val activeChild: ChildProfile,
    val wordStats: List<WordProgress>,
    val exerciseStats: List<ExerciseTypeStats>,
    val settings: ParentSettings,
    val analytics: ParentAnalytics
)

class MainViewModel(
    private val repository: VocabRepository,
    private val stockPhotoRepository: StockPhotoRepository = StockPhotoRepository()
) : ViewModel() {

    private val _appMode = MutableStateFlow(AppMode.CHILD)
    val appMode: StateFlow<AppMode> = _appMode.asStateFlow()

    private val _selectedPackId = MutableStateFlow<String?>(null)
    val selectedPackId: StateFlow<String?> = _selectedPackId.asStateFlow()

    private val _imageGenStates = MutableStateFlow<Map<String, ImageGenUiState>>(emptyMap())
    val imageGenStates: StateFlow<Map<String, ImageGenUiState>> = _imageGenStates.asStateFlow()

    private val _stockPhotoSearchState =
        MutableStateFlow<StockPhotoSearchUiState>(StockPhotoSearchUiState.Idle)
    val stockPhotoSearchState: StateFlow<StockPhotoSearchUiState> =
        _stockPhotoSearchState.asStateFlow()

    private val _isBulkFillingPhotos = MutableStateFlow(false)
    val isBulkFillingPhotos: StateFlow<Boolean> = _isBulkFillingPhotos.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repository.ensureStarterContentIfNeeded()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to seed starter content", e)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val activeChildStatsFlow = repository.observeSettings()
        .map { it.activeChildId.ifBlank { VocabRepository.DEFAULT_CHILD_ID } }
        .distinctUntilChanged()
        .flatMapLatest { childId ->
            combine(
                repository.observeWordStats(childId),
                repository.observeExerciseStats(childId)
            ) { wordStats, exerciseStats ->
                wordStats to exerciseStats
            }
        }

    val dashboardState: StateFlow<UiState<DashboardData>> = combine(
        repository.observePacks(),
        repository.observeChildProfiles(),
        activeChildStatsFlow,
        repository.observeSettings()
    ) { allPacks, childProfiles, (wordStats, exerciseStats), settings ->
        val activeChild = childProfiles.firstOrNull { it.id == settings.activeChildId }
            ?: childProfiles.firstOrNull()
            ?: ChildProfile(
                id = VocabRepository.DEFAULT_CHILD_ID,
                name = settings.childName.ifBlank { "Dítě 1" },
                avatarEmoji = "🦁",
                colorIndex = 0L,
                totalStars = settings.totalStars
            )
        val effectiveProfiles = childProfiles.ifEmpty { listOf(activeChild) }
        val activeChildPacks = allPacks.filter { it.isAssignedTo(activeChild.id) }
        val analytics = computeAnalytics(activeChildPacks, wordStats, exerciseStats)
        DashboardData(
            packs = activeChildPacks,
            allPacks = allPacks,
            childProfiles = effectiveProfiles,
            activeChild = activeChild,
            wordStats = wordStats,
            exerciseStats = exerciseStats,
            settings = settings,
            analytics = analytics
        )
    }
        .map<DashboardData, UiState<DashboardData>> { UiState.Success(it) }
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing dashboard data", error)
            emit(UiState.Error(error.localizedMessage ?: "Nepodařilo se načíst data."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedPackWordsState: StateFlow<UiState<List<VocabWord>>> = _selectedPackId
        .flatMapLatest { packId ->
            if (packId.isNullOrBlank()) {
                flowOf<UiState<List<VocabWord>>>(UiState.Success(emptyList()))
            } else {
                repository.observeWords(packId)
                    .map<List<VocabWord>, UiState<List<VocabWord>>> { UiState.Success(it) }
                    .catch { error ->
                        if (error is CancellationException) throw error
                        Log.w(TAG, "Error observing pack words", error)
                        emit(
                            UiState.Error(
                                error.localizedMessage ?: "Nepodařilo se načíst slovíčka."
                            )
                        )
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    fun setAppMode(mode: AppMode) {
        _appMode.value = mode
    }

    fun selectPack(packId: String?) {
        _selectedPackId.value = packId
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun resetDraftAiStates() {
        _imageGenStates.update { it - DRAFT_KEY }
        _stockPhotoSearchState.value = StockPhotoSearchUiState.Idle
    }

    fun resetStockPhotoSearch() {
        _stockPhotoSearchState.value = StockPhotoSearchUiState.Idle
    }

    fun switchChildProfile(childId: String) {
        viewModelScope.launch {
            try {
                repository.switchActiveChild(childId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to switch child profile", e)
                _actionMessage.value = e.localizedMessage ?: "Nepodařilo se přepnout profil."
            }
        }
    }

    fun createChildProfile(
        name: String,
        avatarEmoji: String,
        colorIndex: Long
    ) {
        viewModelScope.launch {
            try {
                repository.createChildProfile(
                    name = name,
                    avatarEmoji = avatarEmoji,
                    colorIndex = colorIndex,
                    activateImmediately = true
                )
                _actionMessage.value = "Profil \"${name.trim()}\" byl vytvořen."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create child profile", e)
                _actionMessage.value = e.localizedMessage ?: "Nepodařilo se vytvořit profil."
            }
        }
    }

    fun updateChildProfile(
        childId: String,
        name: String,
        avatarEmoji: String,
        colorIndex: Long
    ) {
        viewModelScope.launch {
            try {
                repository.updateChildProfile(
                    childId = childId,
                    name = name,
                    avatarEmoji = avatarEmoji,
                    colorIndex = colorIndex
                )
                _actionMessage.value = "Profil \"${name.trim()}\" byl upraven."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update child profile", e)
                _actionMessage.value = e.localizedMessage ?: "Nepodařilo se upravit profil."
            }
        }
    }

    fun deleteChildProfile(childId: String) {
        viewModelScope.launch {
            try {
                repository.deleteChildProfile(childId)
                _actionMessage.value = "Profil byl smazán."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete child profile", e)
                _actionMessage.value = e.localizedMessage ?: "Nepodařilo se smazat profil."
            }
        }
    }

    fun savePack(
        packId: String?,
        title: String,
        description: String,
        emoji: String,
        colorIndex: Long,
        enabledExercises: List<String>,
        assignedChildIds: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            try {
                if (packId.isNullOrBlank()) {
                    repository.createPack(
                        title = title,
                        description = description,
                        emoji = emoji,
                        colorIndex = colorIndex,
                        enabledExercises = enabledExercises,
                        assignedChildIds = assignedChildIds
                    )
                    _actionMessage.value = "Balíček \"$title\" byl vytvořen."
                } else {
                    repository.updatePack(
                        packId = packId,
                        title = title,
                        description = description,
                        emoji = emoji,
                        colorIndex = colorIndex,
                        enabledExercises = enabledExercises,
                        assignedChildIds = assignedChildIds
                    )
                    _actionMessage.value = "Balíček \"$title\" byl upraven."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save pack", e)
                _actionMessage.value = e.localizedMessage ?: "Nepodařilo se uložit balíček."
            }
        }
    }

    fun updatePackChildAssignments(pack: WordPack, assignedChildIds: List<String>) {
        viewModelScope.launch {
            try {
                repository.updatePack(
                    packId = pack.id,
                    title = pack.title,
                    description = pack.description,
                    emoji = pack.emoji,
                    colorIndex = pack.colorIndex,
                    enabledExercises = pack.enabledExercises,
                    assignedChildIds = assignedChildIds
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update pack child assignment", e)
                _actionMessage.value = e.localizedMessage ?: "Nepodařilo se uložit přiřazení dítěti."
            }
        }
    }

    fun togglePackExercise(pack: WordPack, exerciseType: ExerciseType) {
        val current = pack.enabledExercises.toMutableList()
        if (current.contains(exerciseType.id)) {
            if (current.size <= 1) {
                _actionMessage.value = "Balíček musí mít povolené alespoň jedno cvičení."
                return
            }
            current.remove(exerciseType.id)
        } else {
            current.add(exerciseType.id)
        }
        viewModelScope.launch {
            try {
                repository.updatePack(
                    packId = pack.id,
                    title = pack.title,
                    description = pack.description,
                    emoji = pack.emoji,
                    colorIndex = pack.colorIndex,
                    enabledExercises = current
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle pack exercise", e)
                _actionMessage.value = e.localizedMessage ?: "Nepodařilo se upravit cvičení."
            }
        }
    }

    fun deletePack(packId: String) {
        viewModelScope.launch {
            try {
                repository.deletePack(packId)
                if (_selectedPackId.value == packId) {
                    _selectedPackId.value = null
                }
                _actionMessage.value = "Balíček byl smazán."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete pack", e)
                _actionMessage.value = e.localizedMessage ?: "Nepodařilo se smazat balíček."
            }
        }
    }

    fun saveWord(
        packId: String,
        wordId: String?,
        englishWord: String,
        czechTranslation: String,
        imageBase64: String
    ) {
        viewModelScope.launch {
            try {
                repository.upsertWord(
                    packId = packId,
                    wordId = wordId,
                    englishWord = englishWord,
                    czechTranslation = czechTranslation,
                    exampleSentence = "",
                    imageBase64 = imageBase64
                )
                _actionMessage.value = "Slovíčko \"$englishWord\" bylo uloženo."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save word", e)
                _actionMessage.value = e.localizedMessage ?: "Nepodařilo se uložit slovíčko."
            }
        }
    }

    fun deleteWord(packId: String, wordId: String) {
        viewModelScope.launch {
            try {
                repository.deleteWord(packId, wordId)
                _actionMessage.value = "Slovíčko bylo smazáno."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete word", e)
                _actionMessage.value = e.localizedMessage ?: "Nepodařilo se smazat slovíčko."
            }
        }
    }

    fun searchStockPhotos(query: String) {
        val clean = query.trim()
        if (clean.isBlank()) {
            _stockPhotoSearchState.value = StockPhotoSearchUiState.Error("Zadejte hledané slovo.")
            return
        }
        viewModelScope.launch {
            _stockPhotoSearchState.value = StockPhotoSearchUiState.Loading
            try {
                val results = stockPhotoRepository.searchPhotos(clean)
                _stockPhotoSearchState.value = StockPhotoSearchUiState.Success(
                    query = clean,
                    results = results
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Stock photo search failed", e)
                _stockPhotoSearchState.value = StockPhotoSearchUiState.Error(
                    e.localizedMessage ?: "Nepodařilo se načíst fotobanku. Zkontrolujte připojení."
                )
            }
        }
    }

    fun selectCustomImageForDraft(context: Context, uri: Uri) {
        selectCustomImageInternal(
            context = context,
            key = DRAFT_KEY,
            uri = uri,
            onEncoded = null
        )
    }

    fun selectCustomImageForWord(context: Context, word: VocabWord, uri: Uri) {
        selectCustomImageInternal(
            context = context,
            key = word.id,
            uri = uri,
            onEncoded = { base64 ->
                repository.updateWordImage(wordId = word.id, imageBase64 = base64)
                _actionMessage.value = "Vlastní obrázek pro \"${word.englishWord}\" byl uložen."
            }
        )
    }

    private fun selectCustomImageInternal(
        context: Context,
        key: String,
        uri: Uri,
        onEncoded: (suspend (String) -> Unit)?
    ) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            _imageGenStates.update { it + (key to ImageGenUiState.Loading) }
            try {
                val base64 = withContext(Dispatchers.IO) {
                    com.example.util.ImageCodec.encodeUriToBase64(appContext, uri)
                }
                onEncoded?.invoke(base64)
                _imageGenStates.update { it + (key to ImageGenUiState.Success(base64)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load custom image", e)
                _imageGenStates.update {
                    it + (key to ImageGenUiState.Error(
                        e.localizedMessage ?: "Nepodařilo se načíst vybraný obrázek."
                    ))
                }
            }
        }
    }

    fun selectStockPhotoForDraft(photo: StockPhotoResult) {
        selectStockPhotoInternal(
            key = DRAFT_KEY,
            photo = photo,
            onDownloaded = null
        )
    }

    fun selectStockPhotoForWord(word: VocabWord, photo: StockPhotoResult) {
        selectStockPhotoInternal(
            key = word.id,
            photo = photo,
            onDownloaded = { base64 ->
                repository.updateWordImage(wordId = word.id, imageBase64 = base64)
            }
        )
    }

    fun fillPackWithStockPhotos(words: List<VocabWord>) {
        if (_isBulkFillingPhotos.value) return
        val targets = words.filter { it.imageBase64.isBlank() }
        if (targets.isEmpty()) {
            _actionMessage.value = "Všechna slovíčka v balíčku už mají vlastní fotku."
            return
        }
        viewModelScope.launch {
            _isBulkFillingPhotos.value = true
            var updatedCount = 0
            try {
                for (word in targets) {
                    _imageGenStates.update { it + (word.id to ImageGenUiState.Loading) }
                    try {
                        val results = stockPhotoRepository.searchPhotos(word.englishWord)
                        val first = results.firstOrNull()
                        if (first != null) {
                            val base64 = stockPhotoRepository.downloadAndEncodeBase64(first)
                            repository.updateWordImage(wordId = word.id, imageBase64 = base64)
                            _imageGenStates.update {
                                it + (word.id to ImageGenUiState.Success(base64))
                            }
                            updatedCount += 1
                        } else {
                            _imageGenStates.update { it - word.id }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "Bulk stock photo failed for ${word.englishWord}", e)
                        _imageGenStates.update { it - word.id }
                    }
                }
                _actionMessage.value = if (updatedCount > 0) {
                    "Z databanky bylo doplněno $updatedCount obrázků."
                } else {
                    "Nepodařilo se najít nové obrázky. Zkuste vybrat obrázek ručně u slovíčka."
                }
            } finally {
                _isBulkFillingPhotos.value = false
            }
        }
    }

    private fun selectStockPhotoInternal(
        key: String,
        photo: StockPhotoResult,
        onDownloaded: (suspend (String) -> Unit)?
    ) {
        viewModelScope.launch {
            _imageGenStates.update { it + (key to ImageGenUiState.Loading) }
            try {
                val base64 = stockPhotoRepository.downloadAndEncodeBase64(photo)
                onDownloaded?.invoke(base64)
                _imageGenStates.update { it + (key to ImageGenUiState.Success(base64)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download stock photo", e)
                _imageGenStates.update {
                    it + (key to ImageGenUiState.Error(
                        e.localizedMessage ?: "Nepodařilo se stáhnout vybraný obrázek."
                    ))
                }
            }
        }
    }

    fun saveParentSettings(pinEnabled: Boolean, pin: String) {
        viewModelScope.launch {
            try {
                repository.updateParentSettings(
                    parentPinEnabled = pinEnabled,
                    parentPin = pin
                )
                _actionMessage.value = "Nastavení bylo uloženo."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save settings", e)
                _actionMessage.value = e.localizedMessage ?: "Nepodařilo se uložit nastavení."
            }
        }
    }

    fun recordPracticeSession(
        exerciseType: ExerciseType,
        wordOutcomes: Map<VocabWord, Boolean>,
        starsEarned: Int,
        score: Int
    ) {
        if (wordOutcomes.isEmpty()) return
        val currentChildId = (dashboardState.value as? UiState.Success)?.data?.activeChild?.id
        viewModelScope.launch {
            try {
                repository.recordPracticeResults(
                    childId = currentChildId,
                    exerciseType = exerciseType,
                    wordOutcomes = wordOutcomes,
                    starsEarned = starsEarned,
                    score = score
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record practice results", e)
            }
        }
    }

    private fun computeAnalytics(
        packs: List<WordPack>,
        wordStats: List<WordProgress>,
        exerciseStats: List<ExerciseTypeStats>
    ): ParentAnalytics {
        val totalWordsCount = packs.sumOf { it.wordCount.toInt() }
        val practicedWordsCount = wordStats.count { it.attempts > 0L }
        val learnedWordsCount = wordStats.count { it.isMastered }

        val totalAttempts = wordStats.sumOf { it.attempts }
        val totalCorrect = wordStats.sumOf { it.correctCount }
        val overallAccuracy = if (totalAttempts > 0L) {
            ((totalCorrect * 100L) / totalAttempts).toInt().coerceIn(0, 100)
        } else {
            0
        }

        val weakWords = wordStats
            .filter { it.attempts > 0L && (it.needsReview || it.accuracyPercent < 75) }
            .sortedWith(compareBy<WordProgress> { it.accuracyPercent }.thenByDescending { it.attempts })

        val masteredWords = wordStats
            .filter { it.isMastered }
            .sortedByDescending { it.streak }

        val statsByPack = wordStats.groupBy { it.packId }
        val packSummaries = packs.map { pack ->
            val pStats = statsByPack[pack.id].orEmpty()
            val attempts = pStats.sumOf { it.attempts }
            val correct = pStats.sumOf { it.correctCount }
            val acc = if (attempts > 0L) ((correct * 100L) / attempts).toInt() else 0
            PackProgressSummary(
                pack = pack,
                practicedWordsCount = pStats.count { it.attempts > 0L },
                masteredWordsCount = pStats.count { it.isMastered },
                accuracyPercent = acc
            )
        }.sortedBy { it.accuracyPercent }

        val exStatsMap = exerciseStats.associateBy { it.exerciseType }
        val exerciseBreakdown = ExerciseType.entries.map { type ->
            val stat = exStatsMap[type.id]
            ExerciseStatSummary(
                type = type,
                attempts = stat?.attempts ?: 0L,
                correctCount = stat?.correctCount ?: 0L,
                sessionsCompleted = stat?.sessionsCompleted ?: 0L,
                bestScore = stat?.bestScore ?: 0L,
                accuracyPercent = stat?.accuracyPercent ?: 0
            )
        }

        return ParentAnalytics(
            totalWordsCount = totalWordsCount,
            practicedWordsCount = practicedWordsCount,
            learnedWordsCount = learnedWordsCount,
            overallAccuracyPercent = overallAccuracy,
            weakWords = weakWords,
            masteredWords = masteredWords,
            packSummaries = packSummaries,
            exerciseBreakdown = exerciseBreakdown
        )
    }

    companion object {
        private const val TAG = "MainViewModel"
        private const val STOP_TIMEOUT_MILLIS = 5000L
        const val DRAFT_KEY = "draft"

        fun provideFactory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                MainViewModel(
                    repository = VocabRepository(context.applicationContext),
                    stockPhotoRepository = StockPhotoRepository()
                )
            }
        }
    }
}
