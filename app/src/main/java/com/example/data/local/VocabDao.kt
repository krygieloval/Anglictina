package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.ExerciseType
import com.example.data.model.VocabWord
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabDao {

    @Query("SELECT * FROM child_profiles ORDER BY createdAtMillis ASC")
    fun observeChildProfiles(): Flow<List<ChildProfileEntity>>

    @Query("SELECT * FROM child_profiles ORDER BY createdAtMillis ASC")
    suspend fun getAllChildProfilesOnce(): List<ChildProfileEntity>

    @Query("SELECT * FROM child_profiles WHERE id = :childId LIMIT 1")
    suspend fun getChildProfileById(childId: String): ChildProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChildProfile(profile: ChildProfileEntity)

    @Query("DELETE FROM child_profiles WHERE id = :childId")
    suspend fun deleteChildProfileRow(childId: String)

    @Query("DELETE FROM word_progress WHERE childId = :childId")
    suspend fun deleteWordProgressByChildId(childId: String)

    @Query("DELETE FROM exercise_stats WHERE childId = :childId")
    suspend fun deleteExerciseStatsByChildId(childId: String)

    @Query("SELECT * FROM word_packs ORDER BY createdAtMillis ASC")
    fun observePacks(): Flow<List<WordPackEntity>>

    @Query("SELECT * FROM vocab_words WHERE packId = :packId ORDER BY createdAtMillis ASC")
    fun observeWords(packId: String): Flow<List<VocabWordEntity>>

    @Query("SELECT * FROM word_progress WHERE childId = :childId")
    fun observeWordStatsForChild(childId: String): Flow<List<WordProgressEntity>>

    @Query("SELECT * FROM exercise_stats WHERE childId = :childId")
    fun observeExerciseStatsForChild(childId: String): Flow<List<ExerciseTypeStatsEntity>>

    @Query("SELECT * FROM parent_settings WHERE id = 'parent' LIMIT 1")
    fun observeSettings(): Flow<ParentSettingsEntity?>

    @Query("SELECT * FROM vocab_words WHERE packId = :packId ORDER BY createdAtMillis ASC")
    suspend fun getPackWordsOnce(packId: String): List<VocabWordEntity>

    @Query("SELECT * FROM word_packs WHERE id = :packId LIMIT 1")
    suspend fun getPackById(packId: String): WordPackEntity?

    @Query("SELECT * FROM vocab_words WHERE id = :wordId LIMIT 1")
    suspend fun getWordById(wordId: String): VocabWordEntity?

    @Query("SELECT COUNT(*) FROM vocab_words WHERE packId = :packId")
    suspend fun countWordsInPack(packId: String): Long

    @Query("SELECT * FROM word_packs ORDER BY createdAtMillis ASC")
    suspend fun getAllPacksOnce(): List<WordPackEntity>

    @Query("SELECT * FROM parent_settings WHERE id = 'parent' LIMIT 1")
    suspend fun getSettingsOnce(): ParentSettingsEntity?

    @Query("SELECT * FROM word_progress WHERE id = :progressId LIMIT 1")
    suspend fun getWordProgressOnce(progressId: String): WordProgressEntity?

    @Query("SELECT * FROM exercise_stats WHERE id = :statsId LIMIT 1")
    suspend fun getExerciseStatsOnce(statsId: String): ExerciseTypeStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPack(pack: WordPackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPacks(packs: List<WordPackEntity>)

    @Update
    suspend fun updatePack(pack: WordPackEntity)

    @Query("DELETE FROM word_packs WHERE id = :packId")
    suspend fun deletePackRow(packId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: VocabWordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<VocabWordEntity>)

    @Update
    suspend fun updateWord(word: VocabWordEntity)

    @Query("DELETE FROM vocab_words WHERE id = :wordId")
    suspend fun deleteWordRow(wordId: String)

    @Query("UPDATE vocab_words SET imageBase64 = :imageBase64, updatedAtMillis = :updatedAtMillis WHERE id = :wordId")
    suspend fun updateWordImage(wordId: String, imageBase64: String, updatedAtMillis: Long)

    @Query("UPDATE vocab_words SET exampleSentence = ''")
    suspend fun clearAllExampleSentences()

    @Query("UPDATE word_packs SET wordCount = :wordCount, updatedAtMillis = :updatedAtMillis WHERE id = :packId")
    suspend fun updatePackWordCount(packId: String, wordCount: Long, updatedAtMillis: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWordProgress(progress: WordProgressEntity)

    @Query("DELETE FROM word_progress WHERE wordId = :wordId OR id = :wordId")
    suspend fun deleteWordProgressByWordId(wordId: String)

    @Query("DELETE FROM word_progress WHERE packId = :packId")
    suspend fun deleteWordProgressByPackId(packId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExerciseStats(stats: ExerciseTypeStatsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: ParentSettingsEntity)

    @Transaction
    suspend fun deletePackAndStats(packId: String) {
        deleteWordProgressByPackId(packId)
        deletePackRow(packId)
    }

    @Transaction
    suspend fun deleteChildProfileTransaction(childId: String, nowMillis: Long) {
        val all = getAllChildProfilesOnce()
        if (all.size <= 1) return
        deleteWordProgressByChildId(childId)
        deleteExerciseStatsByChildId(childId)
        deleteChildProfileRow(childId)

        for (pack in getAllPacksOnce()) {
            val currentIds = pack.assignedChildIdsCsv
                .split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            if (childId in currentIds) {
                val updatedIds = currentIds.filterNot { it == childId }
                updatePack(
                    pack.copy(
                        assignedChildIdsCsv = updatedIds.joinToString(","),
                        updatedAtMillis = nowMillis
                    )
                )
            }
        }

        val remaining = getAllChildProfilesOnce()
        val nextActive = remaining.firstOrNull() ?: return
        val settings = getSettingsOnce()
        if (settings != null && settings.activeChildId == childId) {
            upsertSettings(
                settings.copy(
                    activeChildId = nextActive.id,
                    childName = nextActive.name,
                    totalStars = nextActive.totalStars,
                    updatedAtMillis = nowMillis
                )
            )
        }
    }

    @Transaction
    suspend fun switchActiveChildTransaction(childId: String, nowMillis: Long) {
        val target = getChildProfileById(childId) ?: return
        val prevSettings = getSettingsOnce()
        val updated = (prevSettings ?: ParentSettingsEntity(
            id = "parent",
            childName = target.name,
            parentPinEnabled = false,
            parentPin = "",
            totalStars = target.totalStars,
            createdAtMillis = nowMillis,
            updatedAtMillis = nowMillis,
            activeChildId = target.id
        )).copy(
            activeChildId = target.id,
            childName = target.name,
            totalStars = target.totalStars,
            updatedAtMillis = nowMillis
        )
        upsertSettings(updated)
    }

    @Transaction
    suspend fun upsertWordAndUpdateCount(
        word: VocabWordEntity,
        isNew: Boolean,
        nowMillis: Long
    ) {
        if (isNew) {
            insertWord(word)
        } else {
            updateWord(word)
        }
        val count = countWordsInPack(word.packId)
        updatePackWordCount(word.packId, count, nowMillis)
    }

    @Transaction
    suspend fun deleteWordAndUpdateCount(
        packId: String,
        wordId: String,
        nowMillis: Long
    ) {
        deleteWordProgressByWordId(wordId)
        deleteWordRow(wordId)
        val count = countWordsInPack(packId)
        updatePackWordCount(packId, count, nowMillis)
    }

    @Transaction
    suspend fun recordPracticeTransaction(
        childId: String,
        exerciseType: ExerciseType,
        wordOutcomes: Map<VocabWord, Boolean>,
        starsEarned: Int,
        score: Int,
        nowMillis: Long
    ) {
        val resolvedChildId = childId.ifBlank { "default_child" }

        if (exerciseType != ExerciseType.MEMORY) {
            for ((word, isCorrect) in wordOutcomes) {
                val progressId = "${resolvedChildId}_${word.id}"
                val existing = getWordProgressOnce(progressId)
                val updated = if (existing != null) {
                    existing.copy(
                        englishWord = word.englishWord.take(60),
                        czechTranslation = word.czechTranslation.take(60),
                        attempts = existing.attempts + 1L,
                        correctCount = existing.correctCount + if (isCorrect) 1L else 0L,
                        streak = if (isCorrect) existing.streak + 1L else 0L,
                        lastPracticedAtMillis = nowMillis,
                        updatedAtMillis = nowMillis
                    )
                } else {
                    WordProgressEntity(
                        id = progressId,
                        childId = resolvedChildId,
                        wordId = word.id,
                        packId = word.packId,
                        englishWord = word.englishWord.take(60),
                        czechTranslation = word.czechTranslation.take(60),
                        attempts = 1L,
                        correctCount = if (isCorrect) 1L else 0L,
                        streak = if (isCorrect) 1L else 0L,
                        lastPracticedAtMillis = nowMillis,
                        createdAtMillis = nowMillis,
                        updatedAtMillis = nowMillis
                    )
                }
                upsertWordProgress(updated)
            }
        }

        val roundAttempts = if (exerciseType == ExerciseType.MEMORY) 0L else wordOutcomes.size.toLong().coerceAtLeast(1L)
        val roundCorrect = if (exerciseType == ExerciseType.MEMORY) 0L else wordOutcomes.values.count { it }.toLong()
        val statsId = "${resolvedChildId}_${exerciseType.id}"
        val prevEx = getExerciseStatsOnce(statsId)
        val updatedEx = if (prevEx != null) {
            prevEx.copy(
                attempts = prevEx.attempts + roundAttempts,
                correctCount = prevEx.correctCount + roundCorrect,
                sessionsCompleted = prevEx.sessionsCompleted + 1L,
                bestScore = maxOf(prevEx.bestScore, score.toLong()),
                updatedAtMillis = nowMillis
            )
        } else {
            ExerciseTypeStatsEntity(
                id = statsId,
                childId = resolvedChildId,
                exerciseType = exerciseType.id,
                attempts = roundAttempts,
                correctCount = roundCorrect,
                sessionsCompleted = 1L,
                bestScore = score.toLong().coerceAtLeast(0L),
                createdAtMillis = nowMillis,
                updatedAtMillis = nowMillis
            )
        }
        upsertExerciseStats(updatedEx)

        val addedStars = starsEarned.toLong().coerceAtLeast(0L)
        val childProfile = getChildProfileById(resolvedChildId) ?: ChildProfileEntity(
            id = resolvedChildId,
            name = "Dítě 1",
            avatarEmoji = "🦁",
            colorIndex = 0L,
            totalStars = 0L,
            createdAtMillis = nowMillis,
            updatedAtMillis = nowMillis
        )
        val updatedChild = childProfile.copy(
            totalStars = childProfile.totalStars + addedStars,
            updatedAtMillis = nowMillis
        )
        upsertChildProfile(updatedChild)

        val prevSettings = getSettingsOnce()
        if (prevSettings == null || prevSettings.activeChildId == resolvedChildId) {
            val updatedSettings = (prevSettings ?: ParentSettingsEntity(
                id = "parent",
                childName = updatedChild.name,
                parentPinEnabled = false,
                parentPin = "",
                totalStars = 0L,
                createdAtMillis = nowMillis,
                updatedAtMillis = nowMillis,
                activeChildId = resolvedChildId
            )).copy(
                totalStars = updatedChild.totalStars,
                childName = updatedChild.name,
                activeChildId = resolvedChildId,
                updatedAtMillis = nowMillis
            )
            upsertSettings(updatedSettings)
        }
    }

    @Transaction
    suspend fun syncCurriculumIfNeeded(
        defaultSettings: ParentSettingsEntity,
        targetCurriculumVersion: Int,
        packs: List<WordPackEntity>,
        words: List<VocabWordEntity>,
        nowMillis: Long
    ) {
        val profiles = getAllChildProfilesOnce()
        val existingSettings = getSettingsOnce()
        if (profiles.isEmpty()) {
            val initialName = existingSettings?.childName?.trim()
                ?.takeIf { it.isNotEmpty() && !it.equals("Kamaráde", ignoreCase = true) }
                ?: "Dítě 1"
            upsertChildProfile(
                ChildProfileEntity(
                    id = "default_child",
                    name = initialName,
                    avatarEmoji = "🦁",
                    colorIndex = 0L,
                    totalStars = existingSettings?.totalStars ?: 0L,
                    createdAtMillis = nowMillis,
                    updatedAtMillis = nowMillis
                )
            )
        } else {
            val defaultProfile = profiles.firstOrNull { it.id == "default_child" }
            if (defaultProfile != null && defaultProfile.name.equals("Kamaráde", ignoreCase = true)) {
                upsertChildProfile(defaultProfile.copy(name = "Dítě 1", updatedAtMillis = nowMillis))
            }
        }

        if (existingSettings != null && existingSettings.curriculumVersion >= targetCurriculumVersion) {
            if (profiles.none { it.id == existingSettings.activeChildId }) {
                val firstProfile = getAllChildProfilesOnce().firstOrNull()
                if (firstProfile != null) {
                    upsertSettings(
                        existingSettings.copy(
                            activeChildId = firstProfile.id,
                            childName = firstProfile.name,
                            totalStars = firstProfile.totalStars,
                            updatedAtMillis = nowMillis
                        )
                    )
                }
            }
            return
        }

        val currentPacks = getAllPacksOnce().toMutableList()
        // Remove obsolete CROSSWORD id from existing packs and clear old example sentences
        for (p in currentPacks) {
            val filtered = p.enabledExercisesCsv
                .split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.equals("CROSSWORD", ignoreCase = true) }
                .ifEmpty { ExerciseType.defaultIds }
                .joinToString(",")
            if (filtered != p.enabledExercisesCsv) {
                updatePack(p.copy(enabledExercisesCsv = filtered, updatedAtMillis = nowMillis))
            }
        }
        clearAllExampleSentences()

        val legacyTitleRenames = mapOf(
            "unit 1 – hračky a oslava" to ("Unit 1" to "Hračky a oslava"),
            "unit 2 – počasí" to ("Unit 2" to "Počasí"),
            "unit 3 – oblečení" to ("Unit 3" to "Oblečení"),
            "unit 4 – dům a bydlení" to ("Unit 4" to "Dům a bydlení"),
            "unit 5 – činnosti" to ("Unit 5" to "Činnosti"),
            "unit 6 – zvířata" to ("Unit 6" to "Zvířata z učebnice"),
            "unit 7 – jídlo a pití" to ("Unit 7" to "Jídlo a pití"),
            "unit 8 – čísla a hry" to ("Unit 8" to "Čísla 11–20 a hry"),
            "zvířátka" to ("Zvířata" to "Nejznámější zvířátka kolem nás"),
            "ovoce" to ("Ovoce a zelenina" to "Nejznámější druhy ovoce a zeleniny")
        )
        for (idx in currentPacks.indices) {
            val pack = currentPacks[idx]
            val rename = legacyTitleRenames[pack.title.trim().lowercase()]
            if (rename != null && currentPacks.none { it.id != pack.id && it.title.equals(rename.first, ignoreCase = true) }) {
                val renamed = pack.copy(
                    title = rename.first,
                    description = rename.second,
                    updatedAtMillis = nowMillis
                )
                updatePack(renamed)
                currentPacks[idx] = renamed
            }
        }

        // Unit 6 must contain strictly the 10 animals from the textbook; preserve any custom photos for extra animals
        val preservedImages = mutableMapOf<String, String>()
        val extraStarterAnimalsInUnit6 = setOf(
            "cat", "dog", "bird", "fish", "lion", "tiger", "elephant",
            "bear", "rabbit", "monkey", "sheep", "mouse", "owl", "butterfly"
        )
        val unit6Pack = currentPacks.firstOrNull { it.title.equals("Unit 6", ignoreCase = true) }
        if (unit6Pack != null) {
            val u6Words = getPackWordsOnce(unit6Pack.id)
            var removedAny = false
            for (w in u6Words) {
                val enLower = w.englishWord.trim().lowercase()
                if (enLower in extraStarterAnimalsInUnit6) {
                    if (w.imageBase64.isNotBlank()) {
                        preservedImages[enLower] = w.imageBase64
                    }
                    deleteWordProgressByWordId(w.id)
                    deleteWordRow(w.id)
                    removedAny = true
                }
            }
            if (removedAny) {
                val newCount = countWordsInPack(unit6Pack.id)
                updatePackWordCount(unit6Pack.id, newCount, nowMillis)
            }
        }

        // Unit 8 moves numbers 1-10 into the standalone "Čísla 1-10" pack; preserve any custom photos
        val numbersOneToTen = setOf(
            "one", "two", "three", "four", "five",
            "six", "seven", "eight", "nine", "ten"
        )
        val unit8Idx = currentPacks.indexOfFirst { it.title.equals("Unit 8", ignoreCase = true) }
        if (unit8Idx >= 0) {
            val unit8Pack = currentPacks[unit8Idx]
            val u8Words = getPackWordsOnce(unit8Pack.id)
            var removedNumbers = false
            for (w in u8Words) {
                val enLower = w.englishWord.trim().lowercase()
                if (enLower in numbersOneToTen) {
                    if (w.imageBase64.isNotBlank()) {
                        preservedImages[enLower] = w.imageBase64
                    }
                    deleteWordProgressByWordId(w.id)
                    deleteWordRow(w.id)
                    removedNumbers = true
                }
            }
            val newCount = if (removedNumbers) countWordsInPack(unit8Pack.id) else unit8Pack.wordCount
            if (removedNumbers || unit8Pack.description == "Čísla a hry") {
                val updatedUnit8 = unit8Pack.copy(
                    description = "Čísla 11–20 a hry",
                    wordCount = newCount,
                    updatedAtMillis = nowMillis
                )
                updatePack(updatedUnit8)
                currentPacks[unit8Idx] = updatedUnit8
            }
        }

        val wordsByPackId = words.groupBy { it.packId }
        val packsToInsert = mutableListOf<WordPackEntity>()
        val wordsToInsert = mutableListOf<VocabWordEntity>()

        for (templatePack in packs) {
            val existing = currentPacks.firstOrNull {
                it.title.equals(templatePack.title, ignoreCase = true)
            }
            val templateWords = wordsByPackId[templatePack.id].orEmpty().map { w ->
                val savedImg = preservedImages[w.englishWord.trim().lowercase()]
                if (w.imageBase64.isBlank() && !savedImg.isNullOrBlank()) {
                    w.copy(imageBase64 = savedImg)
                } else {
                    w
                }
            }
            if (existing == null) {
                packsToInsert += templatePack
                wordsToInsert += templateWords
            } else {
                val existingWords = getPackWordsOnce(existing.id)
                val existingEnglish = existingWords.map { it.englishWord.trim().lowercase() }.toSet()
                val missingWords = templateWords.filter {
                    it.englishWord.trim().lowercase() !in existingEnglish
                }.map { it.copy(packId = existing.id) }
                if (missingWords.isNotEmpty()) {
                    wordsToInsert += missingWords
                    updatePackWordCount(
                        packId = existing.id,
                        wordCount = (existingWords.size + missingWords.size).toLong(),
                        updatedAtMillis = nowMillis
                    )
                }
            }
        }

        if (packsToInsert.isNotEmpty()) {
            insertPacks(packsToInsert)
        }
        if (wordsToInsert.isNotEmpty()) {
            insertWords(wordsToInsert)
        }

        val refreshedProfiles = getAllChildProfilesOnce()
        val activeProfile = refreshedProfiles.firstOrNull { it.id == existingSettings?.activeChildId }
            ?: refreshedProfiles.firstOrNull()
        val activeId = activeProfile?.id ?: "default_child"
        val resolvedChildName = activeProfile?.name
            ?: existingSettings?.childName?.takeIf { it.isNotBlank() && !it.equals("Kamaráde", ignoreCase = true) }
            ?: "Dítě 1"

        val finalSettings = (existingSettings ?: defaultSettings).copy(
            curriculumVersion = targetCurriculumVersion,
            activeChildId = activeId,
            childName = resolvedChildName,
            updatedAtMillis = nowMillis
        )
        upsertSettings(finalSettings)
    }
}
