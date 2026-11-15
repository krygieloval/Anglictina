package com.example.data.model

enum class ExerciseType(
    val id: String,
    val titleCz: String,
    val shortTitleCz: String,
    val descriptionCz: String,
    val skillTagCz: String
) {
    MATCHING(
        id = "MATCHING",
        titleCz = "Spojovačka",
        shortTitleCz = "Spojovačka",
        descriptionCz = "Propoj anglické slovíčko s obrázkem a českým překladem.",
        skillTagCz = "Čtení a význam"
    ),
    FILL_BLANK(
        id = "FILL_BLANK",
        titleCz = "Doplňovačka",
        shortTitleCz = "Doplňovačka",
        descriptionCz = "Doplň chybějící písmena nebo napiš celé anglické slovíčko.",
        skillTagCz = "Psaná forma"
    ),
    PRONUNCIATION(
        id = "PRONUNCIATION",
        titleCz = "Výslovnost",
        shortTitleCz = "Výslovnost",
        descriptionCz = "Poslechni si výslovnost a řekni slovíčko do mikrofonu.",
        skillTagCz = "Mluvení"
    ),
    SHOOTER(
        id = "SHOOTER",
        titleCz = "Sestřelování slovíček",
        shortTitleCz = "Sestřelování",
        descriptionCz = "Vesmírná raketa! Poslechni si české slovo a sestřel správný asteroid.",
        skillTagCz = "Minihra"
    ),
    MEMORY(
        id = "MEMORY",
        titleCz = "Pexeso",
        shortTitleCz = "Pexeso",
        descriptionCz = "Otoč kartičky a najdi dvojice: obrázek a anglické slovíčko.",
        skillTagCz = "Paměť"
    ),
    ANAGRAM(
        id = "ANAGRAM",
        titleCz = "Anagram",
        shortTitleCz = "Anagram",
        descriptionCz = "Poskládej přeházená písmenka do správného anglického slova.",
        skillTagCz = "Přesmyčky"
    ),
    QUIZ(
        id = "QUIZ",
        titleCz = "Kvíz",
        shortTitleCz = "Kvíz",
        descriptionCz = "Vyber ze 4 možností správný anglický překlad k obrázku.",
        skillTagCz = "Výběr"
    );

    companion object {
        fun fromId(id: String): ExerciseType? = entries.firstOrNull { it.id == id }
        val defaultIds: List<String> = entries.map { it.id }
    }
}

data class ChildProfile(
    val id: String = "",
    val name: String = "Dítě 1",
    val avatarEmoji: String = "🦁",
    val colorIndex: Long = 0L,
    val totalStars: Long = 0L,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)

data class WordPack(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val emoji: String = "📚",
    val colorIndex: Long = 0L,
    val enabledExercises: List<String> = ExerciseType.defaultIds,
    val assignedChildIds: List<String> = emptyList(),
    val wordCount: Long = 0L,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    fun parsedExercises(): List<ExerciseType> =
        enabledExercises.mapNotNull { ExerciseType.fromId(it) }
            .ifEmpty { ExerciseType.entries }

    fun isAssignedTo(childId: String): Boolean =
        assignedChildIds.isEmpty() || childId in assignedChildIds
}

data class VocabWord(
    val id: String = "",
    val packId: String = "",
    val englishWord: String = "",
    val czechTranslation: String = "",
    val exampleSentence: String = "",
    val imageBase64: String = "",
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
)

data class WordProgress(
    val id: String = "",
    val childId: String = "default_child",
    val wordId: String = "",
    val packId: String = "",
    val englishWord: String = "",
    val czechTranslation: String = "",
    val attempts: Long = 0L,
    val correctCount: Long = 0L,
    val streak: Long = 0L,
    val lastPracticedAtMillis: Long? = null,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    val accuracyPercent: Int
        get() = if (attempts <= 0L) 0 else ((correctCount * 100L) / attempts).toInt().coerceIn(0, 100)

    val isMastered: Boolean
        get() = streak >= 3L || (attempts >= 3L && accuracyPercent >= 80)

    val needsReview: Boolean
        get() = attempts >= 2L && accuracyPercent < 65
}

data class ExerciseTypeStats(
    val id: String = "",
    val childId: String = "default_child",
    val exerciseType: String = "",
    val attempts: Long = 0L,
    val correctCount: Long = 0L,
    val sessionsCompleted: Long = 0L,
    val bestScore: Long = 0L,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    val accuracyPercent: Int
        get() = if (attempts <= 0L) 0 else ((correctCount * 100L) / attempts).toInt().coerceIn(0, 100)
}

data class ParentSettings(
    val id: String = "parent",
    val childName: String = "Dítě 1",
    val parentPinEnabled: Boolean = false,
    val parentPin: String = "",
    val totalStars: Long = 0L,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
    val activeChildId: String = "default_child"
)
