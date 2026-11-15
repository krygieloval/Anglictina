package com.example.data.repository

import android.content.Context
import com.example.data.local.ChildProfileEntity
import com.example.data.local.ParentSettingsEntity
import com.example.data.local.SlovickaDatabase
import com.example.data.local.VocabDao
import com.example.data.local.VocabWordEntity
import com.example.data.local.WordPackEntity
import com.example.data.model.ChildProfile
import com.example.data.model.ExerciseType
import com.example.data.model.ExerciseTypeStats
import com.example.data.model.ParentSettings
import com.example.data.model.VocabWord
import com.example.data.model.WordPack
import com.example.data.model.WordProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class VocabRepository(
    private val dao: VocabDao
) {
    constructor(context: Context) : this(
        SlovickaDatabase.getInstance(context).vocabDao()
    )

    fun observeChildProfiles(): Flow<List<ChildProfile>> =
        dao.observeChildProfiles().map { list -> list.map { it.toDomain() } }

    fun observePacks(): Flow<List<WordPack>> =
        dao.observePacks().map { list -> list.map { it.toDomain() } }

    fun observeWords(packId: String): Flow<List<VocabWord>> =
        dao.observeWords(packId).map { list -> list.map { it.toDomain() } }

    fun observeWordStats(childId: String = DEFAULT_CHILD_ID): Flow<List<WordProgress>> =
        dao.observeWordStatsForChild(childId).map { list -> list.map { it.toDomain() } }

    fun observeExerciseStats(childId: String = DEFAULT_CHILD_ID): Flow<List<ExerciseTypeStats>> =
        dao.observeExerciseStatsForChild(childId).map { list -> list.map { it.toDomain() } }

    fun observeSettings(): Flow<ParentSettings> =
        dao.observeSettings().map { entity -> entity?.toDomain() ?: ParentSettings(id = "parent") }

    suspend fun getPackWordsOnce(packId: String): List<VocabWord> =
        dao.getPackWordsOnce(packId).map { it.toDomain() }

    suspend fun createChildProfile(
        name: String,
        avatarEmoji: String,
        colorIndex: Long,
        activateImmediately: Boolean = true
    ): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val cleanName = name.trim().ifBlank { "Kamarád" }.take(40)
        val cleanAvatar = avatarEmoji.trim().ifBlank { "🦁" }.take(16)
        dao.upsertChildProfile(
            ChildProfileEntity(
                id = id,
                name = cleanName,
                avatarEmoji = cleanAvatar,
                colorIndex = colorIndex.coerceIn(0L, 5L),
                totalStars = 0L,
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
        if (activateImmediately) {
            dao.switchActiveChildTransaction(id, now)
        }
        return id
    }

    suspend fun updateChildProfile(
        childId: String,
        name: String,
        avatarEmoji: String,
        colorIndex: Long
    ) {
        val existing = dao.getChildProfileById(childId) ?: return
        val now = System.currentTimeMillis()
        val cleanName = name.trim().ifBlank { existing.name }.take(40)
        val cleanAvatar = avatarEmoji.trim().ifBlank { existing.avatarEmoji }.take(16)
        dao.upsertChildProfile(
            existing.copy(
                name = cleanName,
                avatarEmoji = cleanAvatar,
                colorIndex = colorIndex.coerceIn(0L, 5L),
                updatedAtMillis = now
            )
        )
        val settings = dao.getSettingsOnce()
        if (settings != null && settings.activeChildId == childId) {
            dao.upsertSettings(
                settings.copy(
                    childName = cleanName,
                    updatedAtMillis = now
                )
            )
        }
    }

    suspend fun switchActiveChild(childId: String) {
        dao.switchActiveChildTransaction(childId, System.currentTimeMillis())
    }

    suspend fun deleteChildProfile(childId: String) {
        dao.deleteChildProfileTransaction(childId, System.currentTimeMillis())
    }

    suspend fun createPack(
        title: String,
        description: String,
        emoji: String,
        colorIndex: Long,
        enabledExercises: List<String>,
        assignedChildIds: List<String> = emptyList()
    ): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val cleanExercises = enabledExercises
            .filter { ExerciseType.fromId(it) != null }
            .ifEmpty { ExerciseType.defaultIds }
            .distinct()
        val cleanChildren = assignedChildIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val entity = WordPackEntity(
            id = id,
            title = title.trim().take(60),
            description = description.trim().take(200),
            emoji = emoji.trim().ifBlank { "📚" }.take(16),
            colorIndex = colorIndex.coerceIn(0L, 5L),
            enabledExercisesCsv = cleanExercises.joinToString(","),
            wordCount = 0L,
            createdAtMillis = now,
            updatedAtMillis = now,
            assignedChildIdsCsv = cleanChildren.joinToString(",")
        )
        dao.insertPack(entity)
        return id
    }

    suspend fun updatePack(
        packId: String,
        title: String,
        description: String,
        emoji: String,
        colorIndex: Long,
        enabledExercises: List<String>,
        assignedChildIds: List<String>? = null
    ) {
        val existing = dao.getPackById(packId) ?: return
        val now = System.currentTimeMillis()
        val cleanExercises = enabledExercises
            .filter { ExerciseType.fromId(it) != null }
            .ifEmpty { ExerciseType.defaultIds }
            .distinct()
        val resolvedChildrenCsv = assignedChildIds?.let { list ->
            list.map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString(",")
        } ?: existing.assignedChildIdsCsv
        dao.updatePack(
            existing.copy(
                title = title.trim().take(60),
                description = description.trim().take(200),
                emoji = emoji.trim().ifBlank { "📚" }.take(16),
                colorIndex = colorIndex.coerceIn(0L, 5L),
                enabledExercisesCsv = cleanExercises.joinToString(","),
                updatedAtMillis = now,
                assignedChildIdsCsv = resolvedChildrenCsv
            )
        )
    }

    suspend fun deletePack(packId: String) {
        dao.deletePackAndStats(packId)
    }

    suspend fun upsertWord(
        packId: String,
        wordId: String?,
        englishWord: String,
        czechTranslation: String,
        exampleSentence: String = "",
        imageBase64: String
    ): String {
        val now = System.currentTimeMillis()
        return if (wordId.isNullOrBlank()) {
            val newId = UUID.randomUUID().toString()
            val entity = VocabWordEntity(
                id = newId,
                packId = packId,
                englishWord = englishWord.trim().take(60),
                czechTranslation = czechTranslation.trim().take(60),
                exampleSentence = "",
                imageBase64 = imageBase64,
                createdAtMillis = now,
                updatedAtMillis = now
            )
            dao.upsertWordAndUpdateCount(entity, isNew = true, nowMillis = now)
            newId
        } else {
            val existing = dao.getWordById(wordId)
            val entity = VocabWordEntity(
                id = wordId,
                packId = existing?.packId ?: packId,
                englishWord = englishWord.trim().take(60),
                czechTranslation = czechTranslation.trim().take(60),
                exampleSentence = "",
                imageBase64 = imageBase64,
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now
            )
            dao.upsertWordAndUpdateCount(entity, isNew = existing == null, nowMillis = now)
            wordId
        }
    }

    suspend fun updateWordImage(
        wordId: String,
        imageBase64: String
    ) {
        dao.updateWordImage(
            wordId = wordId,
            imageBase64 = imageBase64,
            updatedAtMillis = System.currentTimeMillis()
        )
    }

    suspend fun deleteWord(packId: String, wordId: String) {
        dao.deleteWordAndUpdateCount(
            packId = packId,
            wordId = wordId,
            nowMillis = System.currentTimeMillis()
        )
    }

    suspend fun updateParentSettings(
        parentPinEnabled: Boolean,
        parentPin: String,
        childName: String? = null
    ) {
        val now = System.currentTimeMillis()
        val existing = dao.getSettingsOnce()
        val activeChildId = existing?.activeChildId ?: DEFAULT_CHILD_ID
        val cleanPin = parentPin.filter { it.isDigit() }.take(6)
        val resolvedName = childName?.trim()?.ifBlank { null }?.take(40)
            ?: existing?.childName
            ?: "Dítě 1"
        val entity = if (existing != null) {
            existing.copy(
                childName = resolvedName,
                parentPinEnabled = parentPinEnabled,
                parentPin = cleanPin,
                updatedAtMillis = now
            )
        } else {
            ParentSettingsEntity(
                id = "parent",
                childName = resolvedName,
                parentPinEnabled = parentPinEnabled,
                parentPin = cleanPin,
                totalStars = 0L,
                createdAtMillis = now,
                updatedAtMillis = now,
                activeChildId = activeChildId
            )
        }
        dao.upsertSettings(entity)
    }

    suspend fun recordPracticeResults(
        childId: String? = null,
        exerciseType: ExerciseType,
        wordOutcomes: Map<VocabWord, Boolean>,
        starsEarned: Int,
        score: Int
    ) {
        if (wordOutcomes.isEmpty()) return
        val resolvedChildId = childId ?: dao.getSettingsOnce()?.activeChildId ?: DEFAULT_CHILD_ID
        dao.recordPracticeTransaction(
            childId = resolvedChildId,
            exerciseType = exerciseType,
            wordOutcomes = wordOutcomes,
            starsEarned = starsEarned,
            score = score,
            nowMillis = System.currentTimeMillis()
        )
    }

    suspend fun ensureStarterContentIfNeeded() {
        val baseTime = System.currentTimeMillis()
        val settings = ParentSettingsEntity(
            id = "parent",
            childName = "Dítě 1",
            parentPinEnabled = false,
            parentPin = "",
            totalStars = 0L,
            createdAtMillis = baseTime,
            updatedAtMillis = baseTime,
            curriculumVersion = CURRICULUM_VERSION,
            activeChildId = DEFAULT_CHILD_ID
        )

        data class SeedWord(val en: String, val cz: String)
        data class SeedPack(
            val title: String,
            val desc: String,
            val emoji: String,
            val color: Long,
            val words: List<SeedWord>
        )

        val starterPacks = listOf(
            SeedPack(
                title = "Unit 1",
                desc = "Hračky a oslava",
                emoji = "🎁",
                color = 0L,
                words = listOf(
                    SeedWord("Balloon", "balonek"),
                    SeedWord("Basketball", "basketbalový míč"),
                    SeedWord("Cake", "dort"),
                    SeedWord("Candle", "svíčka"),
                    SeedWord("Car", "auto"),
                    SeedWord("Card", "přání"),
                    SeedWord("Clown", "klaun"),
                    SeedWord("Present", "dárek"),
                    SeedWord("Puzzle", "puzzle"),
                    SeedWord("Robot", "robot")
                )
            ),
            SeedPack(
                title = "Unit 2",
                desc = "Počasí",
                emoji = "☀️",
                color = 1L,
                words = listOf(
                    SeedWord("Cloudy", "zatažený"),
                    SeedWord("Cold", "chladný"),
                    SeedWord("Dry", "suchý"),
                    SeedWord("Hot", "horký"),
                    SeedWord("Rainy", "deštivý"),
                    SeedWord("Snowy", "zasněžený"),
                    SeedWord("Stormy", "bouřkový"),
                    SeedWord("Sunny", "slunečný"),
                    SeedWord("Wet", "mokrý"),
                    SeedWord("Windy", "větrný")
                )
            ),
            SeedPack(
                title = "Unit 3",
                desc = "Oblečení",
                emoji = "👕",
                color = 2L,
                words = listOf(
                    SeedWord("Boots", "vysoké boty"),
                    SeedWord("Hat", "klobouk"),
                    SeedWord("Jacket", "bunda"),
                    SeedWord("Shoes", "boty"),
                    SeedWord("Socks", "ponožky"),
                    SeedWord("Trousers", "kalhoty"),
                    SeedWord("Shorts", "kraťasy"),
                    SeedWord("Skirt", "sukně"),
                    SeedWord("Sweater", "svetr"),
                    SeedWord("T-shirt", "tričko")
                )
            ),
            SeedPack(
                title = "Unit 4",
                desc = "Dům a bydlení",
                emoji = "🏠",
                color = 3L,
                words = listOf(
                    SeedWord("Bath", "vana"),
                    SeedWord("Bathroom", "koupelna"),
                    SeedWord("Bed", "postel"),
                    SeedWord("Bedroom", "ložnice"),
                    SeedWord("Dining room", "jídelna"),
                    SeedWord("Garden", "zahrada"),
                    SeedWord("Kitchen", "kuchyně"),
                    SeedWord("Living room", "obývací pokoj"),
                    SeedWord("Sofa", "pohovka"),
                    SeedWord("Table", "stůl")
                )
            ),
            SeedPack(
                title = "Unit 5",
                desc = "Činnosti",
                emoji = "🏃",
                color = 4L,
                words = listOf(
                    SeedWord("Catch", "chytit"),
                    SeedWord("Climb", "lézt"),
                    SeedWord("Cook", "vařit"),
                    SeedWord("Dance", "tančit"),
                    SeedWord("Dive", "skákat do vody"),
                    SeedWord("Fly", "létat"),
                    SeedWord("Jump", "skákat"),
                    SeedWord("Run", "běžet"),
                    SeedWord("Sing", "zpívat"),
                    SeedWord("Swim", "plavat")
                )
            ),
            SeedPack(
                title = "Unit 6",
                desc = "Zvířata z učebnice",
                emoji = "🦊",
                color = 5L,
                words = listOf(
                    SeedWord("Bat", "netopýr"),
                    SeedWord("Chicken", "kuře"),
                    SeedWord("Cow", "kráva"),
                    SeedWord("Duck", "kachna"),
                    SeedWord("Fox", "liška"),
                    SeedWord("Frog", "žába"),
                    SeedWord("Goat", "koza"),
                    SeedWord("Horse", "kůň"),
                    SeedWord("Pig", "prase"),
                    SeedWord("Squirrel", "veverka")
                )
            ),
            SeedPack(
                title = "Unit 7",
                desc = "Jídlo a pití",
                emoji = "🍕",
                color = 0L,
                words = listOf(
                    SeedWord("Cheese", "sýr"),
                    SeedWord("Chicken", "kuřecí maso"),
                    SeedWord("Chips", "hranolky"),
                    SeedWord("Ice cream", "zmrzlina"),
                    SeedWord("Pasta", "těstoviny"),
                    SeedWord("Milk", "mléko"),
                    SeedWord("Rice", "rýže"),
                    SeedWord("Salad", "salát"),
                    SeedWord("Sweets", "sladkosti"),
                    SeedWord("Water", "voda")
                )
            ),
            SeedPack(
                title = "Unit 8",
                desc = "Čísla 11–20 a hry",
                emoji = "🔢",
                color = 1L,
                words = listOf(
                    SeedWord("Eleven", "jedenáct"),
                    SeedWord("Twelve", "dvanáct"),
                    SeedWord("Thirteen", "třináct"),
                    SeedWord("Fourteen", "čtrnáct"),
                    SeedWord("Fifteen", "patnáct"),
                    SeedWord("Sixteen", "šestnáct"),
                    SeedWord("Seventeen", "sedmnáct"),
                    SeedWord("Eighteen", "osmnáct"),
                    SeedWord("Nineteen", "devatenáct"),
                    SeedWord("Twenty", "dvacet"),
                    SeedWord("Hula hoop", "obruč"),
                    SeedWord("Skateboard", "skateboard"),
                    SeedWord("Skipping rope", "švihadlo"),
                    SeedWord("Trampoline", "trampolína")
                )
            ),
            SeedPack(
                title = "Čísla 1-10",
                desc = "Základní čísla od jedné do deseti",
                emoji = "1️⃣",
                color = 5L,
                words = listOf(
                    SeedWord("One", "jedna"),
                    SeedWord("Two", "dva"),
                    SeedWord("Three", "tři"),
                    SeedWord("Four", "čtyři"),
                    SeedWord("Five", "pět"),
                    SeedWord("Six", "šest"),
                    SeedWord("Seven", "sedm"),
                    SeedWord("Eight", "osm"),
                    SeedWord("Nine", "devět"),
                    SeedWord("Ten", "deset")
                )
            ),
            SeedPack(
                title = "Zvířata",
                desc = "Nejznámější zvířátka kolem nás",
                emoji = "🐾",
                color = 4L,
                words = listOf(
                    SeedWord("Cat", "kočka"),
                    SeedWord("Dog", "pes"),
                    SeedWord("Bird", "pták"),
                    SeedWord("Fish", "ryba"),
                    SeedWord("Rabbit", "králík"),
                    SeedWord("Mouse", "myš"),
                    SeedWord("Cow", "kráva"),
                    SeedWord("Horse", "kůň"),
                    SeedWord("Pig", "prase"),
                    SeedWord("Sheep", "ovce"),
                    SeedWord("Duck", "kachna"),
                    SeedWord("Frog", "žába"),
                    SeedWord("Lion", "lev"),
                    SeedWord("Tiger", "tygr"),
                    SeedWord("Elephant", "slon"),
                    SeedWord("Bear", "medvěd"),
                    SeedWord("Monkey", "opice"),
                    SeedWord("Giraffe", "žirafa"),
                    SeedWord("Turtle", "želva"),
                    SeedWord("Snake", "had"),
                    SeedWord("Whale", "velryba"),
                    SeedWord("Owl", "sova"),
                    SeedWord("Butterfly", "motýl"),
                    SeedWord("Bee", "včela")
                )
            ),
            SeedPack(
                title = "Barvy",
                desc = "Základní anglické barvy kolem nás",
                emoji = "🎨",
                color = 2L,
                words = listOf(
                    SeedWord("Red", "červená"),
                    SeedWord("Blue", "modrá"),
                    SeedWord("Green", "zelená"),
                    SeedWord("Yellow", "žlutá"),
                    SeedWord("Orange", "oranžová"),
                    SeedWord("Purple", "fialová"),
                    SeedWord("Pink", "růžová"),
                    SeedWord("Brown", "hnědá"),
                    SeedWord("Black", "černá"),
                    SeedWord("White", "bílá"),
                    SeedWord("Grey", "šedá")
                )
            ),
            SeedPack(
                title = "Ovoce a zelenina",
                desc = "Nejznámější druhy ovoce a zeleniny",
                emoji = "🍎",
                color = 3L,
                words = listOf(
                    SeedWord("Apple", "jablko"),
                    SeedWord("Banana", "banán"),
                    SeedWord("Orange", "pomeranč"),
                    SeedWord("Pear", "hruška"),
                    SeedWord("Cherry", "třešeň"),
                    SeedWord("Strawberry", "jahoda"),
                    SeedWord("Watermelon", "meloun"),
                    SeedWord("Grapes", "hrozny"),
                    SeedWord("Lemon", "citron"),
                    SeedWord("Peach", "broskev"),
                    SeedWord("Pineapple", "ananas"),
                    SeedWord("Blueberry", "borůvka"),
                    SeedWord("Kiwi", "kiwi"),
                    SeedWord("Avocado", "avokádo"),
                    SeedWord("Carrot", "mrkev"),
                    SeedWord("Tomato", "rajče"),
                    SeedWord("Potato", "brambora"),
                    SeedWord("Cucumber", "okurka"),
                    SeedWord("Pepper", "paprika"),
                    SeedWord("Onion", "cibule"),
                    SeedWord("Broccoli", "brokolice"),
                    SeedWord("Corn", "kukuřice"),
                    SeedWord("Peas", "hrášek"),
                    SeedWord("Garlic", "česnek"),
                    SeedWord("Eggplant", "lilek")
                )
            )
        )

        val packEntities = mutableListOf<WordPackEntity>()
        val wordEntities = mutableListOf<VocabWordEntity>()
        var offset = 0L

        for (seedPack in starterPacks) {
            val packId = UUID.randomUUID().toString()
            val packTime = baseTime + offset++
            packEntities += WordPackEntity(
                id = packId,
                title = seedPack.title,
                description = seedPack.desc,
                emoji = seedPack.emoji,
                colorIndex = seedPack.color,
                enabledExercisesCsv = ExerciseType.defaultIds.joinToString(","),
                wordCount = seedPack.words.size.toLong(),
                createdAtMillis = packTime,
                updatedAtMillis = packTime
            )
            for (w in seedPack.words) {
                val wordTime = baseTime + offset++
                wordEntities += VocabWordEntity(
                    id = UUID.randomUUID().toString(),
                    packId = packId,
                    englishWord = w.en,
                    czechTranslation = w.cz,
                    exampleSentence = "",
                    imageBase64 = "",
                    createdAtMillis = wordTime,
                    updatedAtMillis = wordTime
                )
            }
        }

        dao.syncCurriculumIfNeeded(
            defaultSettings = settings,
            targetCurriculumVersion = CURRICULUM_VERSION,
            packs = packEntities,
            words = wordEntities,
            nowMillis = baseTime
        )
    }

    companion object {
        const val CURRICULUM_VERSION = 6
        const val DEFAULT_CHILD_ID = "default_child"
    }
}
