package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.ChildProfile
import com.example.data.model.ExerciseType
import com.example.data.model.ExerciseTypeStats
import com.example.data.model.ParentSettings
import com.example.data.model.VocabWord
import com.example.data.model.WordPack
import com.example.data.model.WordProgress

@Entity(tableName = "child_profiles")
data class ChildProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val avatarEmoji: String,
    val colorIndex: Long,
    val totalStars: Long,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    fun toDomain(): ChildProfile = ChildProfile(
        id = id,
        name = name,
        avatarEmoji = avatarEmoji,
        colorIndex = colorIndex,
        totalStars = totalStars,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

@Entity(tableName = "word_packs")
data class WordPackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val colorIndex: Long,
    val enabledExercisesCsv: String,
    val wordCount: Long,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val assignedChildIdsCsv: String = ""
) {
    fun toDomain(): WordPack {
        val exercises = enabledExercisesCsv
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() && ExerciseType.fromId(it) != null }
            .ifEmpty { ExerciseType.defaultIds }
        val assignedChildren = assignedChildIdsCsv
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        return WordPack(
            id = id,
            title = title,
            description = description,
            emoji = emoji,
            colorIndex = colorIndex,
            enabledExercises = exercises,
            assignedChildIds = assignedChildren,
            wordCount = wordCount,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = updatedAtMillis
        )
    }
}

@Entity(
    tableName = "vocab_words",
    foreignKeys = [
        ForeignKey(
            entity = WordPackEntity::class,
            parentColumns = ["id"],
            childColumns = ["packId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("packId")]
)
data class VocabWordEntity(
    @PrimaryKey val id: String,
    val packId: String,
    val englishWord: String,
    val czechTranslation: String,
    val exampleSentence: String,
    val imageBase64: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    fun toDomain(): VocabWord = VocabWord(
        id = id,
        packId = packId,
        englishWord = englishWord,
        czechTranslation = czechTranslation,
        exampleSentence = exampleSentence,
        imageBase64 = imageBase64,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

@Entity(
    tableName = "word_progress",
    indices = [Index("packId"), Index("childId"), Index("wordId")]
)
data class WordProgressEntity(
    @PrimaryKey val id: String,
    val childId: String = "default_child",
    val wordId: String = "",
    val packId: String,
    val englishWord: String,
    val czechTranslation: String,
    val attempts: Long,
    val correctCount: Long,
    val streak: Long,
    val lastPracticedAtMillis: Long?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    fun toDomain(): WordProgress = WordProgress(
        id = id,
        childId = childId,
        wordId = wordId,
        packId = packId,
        englishWord = englishWord,
        czechTranslation = czechTranslation,
        attempts = attempts,
        correctCount = correctCount,
        streak = streak,
        lastPracticedAtMillis = lastPracticedAtMillis,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

@Entity(
    tableName = "exercise_stats",
    indices = [Index("childId")]
)
data class ExerciseTypeStatsEntity(
    @PrimaryKey val id: String,
    val childId: String = "default_child",
    val exerciseType: String,
    val attempts: Long,
    val correctCount: Long,
    val sessionsCompleted: Long,
    val bestScore: Long,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    fun toDomain(): ExerciseTypeStats = ExerciseTypeStats(
        id = id,
        childId = childId,
        exerciseType = exerciseType,
        attempts = attempts,
        correctCount = correctCount,
        sessionsCompleted = sessionsCompleted,
        bestScore = bestScore,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

@Entity(tableName = "parent_settings")
data class ParentSettingsEntity(
    @PrimaryKey val id: String = "parent",
    val childName: String,
    val parentPinEnabled: Boolean,
    val parentPin: String,
    val totalStars: Long,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val curriculumVersion: Int = 1,
    val activeChildId: String = "default_child"
) {
    fun toDomain(): ParentSettings = ParentSettings(
        id = id,
        childName = childName,
        parentPinEnabled = parentPinEnabled,
        parentPin = parentPin,
        totalStars = totalStars,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis,
        activeChildId = activeChildId
    )
}
