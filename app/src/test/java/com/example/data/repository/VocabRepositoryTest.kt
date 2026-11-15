package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SlovickaDatabase
import com.example.data.model.ExerciseType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VocabRepositoryTest {

    private lateinit var database: SlovickaDatabase
    private lateinit var repository: VocabRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SlovickaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = VocabRepository(database.vocabDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `seed starter content creates units 1 to 8 plus colors and fruit without extra sentences`() = runBlocking {
        repository.ensureStarterContentIfNeeded()
        repository.ensureStarterContentIfNeeded() // idempotent

        assertFalse(ExerciseType.defaultIds.contains("CROSSWORD"))
        assertEquals("Výslovnost", ExerciseType.PRONUNCIATION.titleCz)

        val profiles = repository.observeChildProfiles().first()
        assertEquals(1, profiles.size)
        assertEquals(VocabRepository.DEFAULT_CHILD_ID, profiles[0].id)
        assertEquals("Dítě 1", profiles[0].name)

        val packs = repository.observePacks().first()
        assertEquals(12, packs.size)
        assertTrue(packs.any { it.title == "Unit 1" })
        assertTrue(packs.any { it.title == "Unit 2" })

        val unit6Pack = packs.first { it.title == "Unit 6" }
        assertEquals(10L, unit6Pack.wordCount)
        val unit6Words = repository.observeWords(unit6Pack.id).first()
        assertEquals(
            setOf("Bat", "Chicken", "Cow", "Duck", "Fox", "Frog", "Goat", "Horse", "Pig", "Squirrel"),
            unit6Words.map { it.englishWord }.toSet()
        )
        assertTrue(unit6Words.all { it.exampleSentence.isEmpty() })

        val animalsPack = packs.first { it.title == "Zvířata" }
        assertTrue(animalsPack.wordCount >= 20L)
        val animalWords = repository.observeWords(animalsPack.id).first()
        assertEquals(24, animalWords.size)
        assertTrue(animalWords.any { it.englishWord == "Cat" })
        assertTrue(animalWords.any { it.englishWord == "Lion" })

        val unit8Pack = packs.first { it.title == "Unit 8" }
        val unit8Words = repository.observeWords(unit8Pack.id).first()
        assertEquals(14, unit8Words.size)
        assertFalse(unit8Words.any { it.englishWord == "One" })
        assertFalse(unit8Words.any { it.englishWord == "Ten" })
        assertTrue(unit8Words.any { it.englishWord == "Eleven" })
        assertTrue(unit8Words.any { it.englishWord == "Trampoline" })

        val numbersOneToTenPack = packs.first { it.title == "Čísla 1-10" }
        val numbersOneToTenWords = repository.observeWords(numbersOneToTenPack.id).first()
        assertEquals(10, numbersOneToTenWords.size)
        assertTrue(numbersOneToTenWords.any { it.englishWord == "One" })
        assertTrue(numbersOneToTenWords.any { it.englishWord == "Ten" })

        val colorsPack = packs.first { it.title == "Barvy" }
        assertEquals(11L, colorsPack.wordCount)

        val fruitVegPack = packs.first { it.title == "Ovoce a zelenina" }
        val fruitVegWords = repository.observeWords(fruitVegPack.id).first()
        assertEquals(25, fruitVegWords.size)
        assertTrue(fruitVegWords.any { it.englishWord == "Apple" })
        assertTrue(fruitVegWords.any { it.englishWord == "Strawberry" })
        assertTrue(fruitVegWords.any { it.englishWord == "Carrot" })
        assertTrue(fruitVegWords.any { it.englishWord == "Eggplant" })
    }

    @Test
    fun `legacy starter database upgrades animals pack strips crossword and clears sentences`() = runBlocking {
        val now = System.currentTimeMillis()
        val dao = database.vocabDao()
        dao.upsertSettings(
            com.example.data.local.ParentSettingsEntity(
                id = "parent",
                childName = "Ema",
                parentPinEnabled = false,
                parentPin = "",
                totalStars = 12L,
                createdAtMillis = now,
                updatedAtMillis = now,
                curriculumVersion = 2
            )
        )
        dao.insertPacks(
            listOf(
                com.example.data.local.WordPackEntity(
                    id = "legacy-unit6",
                    title = "Unit 6 – Zvířata",
                    description = "Stará zvířátka",
                    emoji = "🐾",
                    colorIndex = 0L,
                    enabledExercisesCsv = "MATCHING,CROSSWORD,QUIZ",
                    wordCount = 2L,
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            )
        )
        dao.insertWords(
            listOf(
                com.example.data.local.VocabWordEntity(
                    id = "legacy-cat",
                    packId = "legacy-unit6",
                    englishWord = "Cat",
                    czechTranslation = "Kočka",
                    exampleSentence = "The cat likes milk.",
                    imageBase64 = "custom-img",
                    createdAtMillis = now,
                    updatedAtMillis = now
                ),
                com.example.data.local.VocabWordEntity(
                    id = "legacy-cow",
                    packId = "legacy-unit6",
                    englishWord = "Cow",
                    czechTranslation = "kráva",
                    exampleSentence = "The cow says moo.",
                    imageBase64 = "",
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            )
        )

        repository.ensureStarterContentIfNeeded()

        val packs = repository.observePacks().first()
        assertEquals(12, packs.size)
        val upgradedUnit6 = packs.first { it.id == "legacy-unit6" }
        assertEquals("Unit 6", upgradedUnit6.title)
        assertEquals(10L, upgradedUnit6.wordCount)
        assertFalse(upgradedUnit6.enabledExercises.contains("CROSSWORD"))

        val unit6Words = repository.observeWords("legacy-unit6").first()
        assertFalse(unit6Words.any { it.englishWord == "Cat" })
        assertEquals("", unit6Words.first { it.englishWord == "Cow" }.exampleSentence)

        val animalsPack = packs.first { it.title == "Zvířata" }
        val animalWords = repository.observeWords(animalsPack.id).first()
        assertEquals(24, animalWords.size)
        assertEquals("custom-img", animalWords.first { it.englishWord == "Cat" }.imageBase64)

        val profiles = repository.observeChildProfiles().first()
        assertEquals(1, profiles.size)
        assertEquals("Ema", profiles[0].name)
        assertEquals(12L, profiles[0].totalStars)
    }

    @Test
    fun `legacy v5 database splits Unit 8 numbers 1-10 and renames Ovoce to Ovoce a zelenina`() = runBlocking {
        val now = System.currentTimeMillis()
        val dao = database.vocabDao()
        dao.upsertSettings(
            com.example.data.local.ParentSettingsEntity(
                id = "parent",
                childName = "Dítě 1",
                parentPinEnabled = false,
                parentPin = "",
                totalStars = 0L,
                createdAtMillis = now,
                updatedAtMillis = now,
                curriculumVersion = 5,
                activeChildId = VocabRepository.DEFAULT_CHILD_ID
            )
        )
        dao.insertPacks(
            listOf(
                com.example.data.local.WordPackEntity(
                    id = "legacy-unit8",
                    title = "Unit 8",
                    description = "Čísla a hry",
                    emoji = "🔢",
                    colorIndex = 1L,
                    enabledExercisesCsv = "MATCHING,QUIZ",
                    wordCount = 2L,
                    createdAtMillis = now,
                    updatedAtMillis = now
                ),
                com.example.data.local.WordPackEntity(
                    id = "legacy-ovoce",
                    title = "Ovoce",
                    description = "Sladké ovoce plné vitamínů",
                    emoji = "🍎",
                    colorIndex = 3L,
                    enabledExercisesCsv = "MATCHING,QUIZ",
                    wordCount = 1L,
                    createdAtMillis = now + 1,
                    updatedAtMillis = now + 1
                )
            )
        )
        dao.insertWords(
            listOf(
                com.example.data.local.VocabWordEntity(
                    id = "legacy-one",
                    packId = "legacy-unit8",
                    englishWord = "One",
                    czechTranslation = "jedna",
                    exampleSentence = "",
                    imageBase64 = "custom-one-img",
                    createdAtMillis = now,
                    updatedAtMillis = now
                ),
                com.example.data.local.VocabWordEntity(
                    id = "legacy-eleven",
                    packId = "legacy-unit8",
                    englishWord = "Eleven",
                    czechTranslation = "jedenáct",
                    exampleSentence = "",
                    imageBase64 = "",
                    createdAtMillis = now + 1,
                    updatedAtMillis = now + 1
                ),
                com.example.data.local.VocabWordEntity(
                    id = "legacy-apple",
                    packId = "legacy-ovoce",
                    englishWord = "Apple",
                    czechTranslation = "jablko",
                    exampleSentence = "",
                    imageBase64 = "",
                    createdAtMillis = now + 2,
                    updatedAtMillis = now + 2
                )
            )
        )

        repository.ensureStarterContentIfNeeded()

        val packs = repository.observePacks().first()
        assertEquals(12, packs.size)

        val unit8 = packs.first { it.id == "legacy-unit8" }
        assertEquals("Čísla 11–20 a hry", unit8.description)
        assertEquals(14L, unit8.wordCount)
        val unit8Words = repository.observeWords(unit8.id).first()
        assertFalse(unit8Words.any { it.englishWord == "One" })
        assertTrue(unit8Words.any { it.englishWord == "Eleven" })

        val numbersOneToTen = packs.first { it.title == "Čísla 1-10" }
        val numbersWords = repository.observeWords(numbersOneToTen.id).first()
        assertEquals(10, numbersWords.size)
        assertEquals("custom-one-img", numbersWords.first { it.englishWord == "One" }.imageBase64)

        val fruitVeg = packs.first { it.id == "legacy-ovoce" }
        assertEquals("Ovoce a zelenina", fruitVeg.title)
        assertEquals(25L, fruitVeg.wordCount)
        val fruitVegWords = repository.observeWords(fruitVeg.id).first()
        assertEquals(25, fruitVegWords.size)
        assertTrue(fruitVegWords.any { it.englishWord == "Carrot" })
    }

    @Test
    fun `multiple child profiles isolate stars word progress and exercise stats`() = runBlocking {
        repository.ensureStarterContentIfNeeded()

        val packId = repository.createPack(
            title = "Test",
            description = "",
            emoji = "🎨",
            colorIndex = 2L,
            enabledExercises = listOf(ExerciseType.MATCHING.id, ExerciseType.QUIZ.id)
        )
        val wordId = repository.upsertWord(
            packId = packId,
            wordId = null,
            englishWord = "Blue",
            czechTranslation = "Modrá",
            imageBase64 = ""
        )
        val words = repository.observeWords(packId).first()
        assertEquals(1, words.size)

        // Child 1 practices
        repository.recordPracticeResults(
            childId = VocabRepository.DEFAULT_CHILD_ID,
            exerciseType = ExerciseType.QUIZ,
            wordOutcomes = mapOf(words[0] to true),
            starsEarned = 3,
            score = 90
        )

        // Create Child 2 (auto-activated)
        val child2Id = repository.createChildProfile(
            name = "Anička",
            avatarEmoji = "🦊",
            colorIndex = 1L,
            activateImmediately = true
        )

        assertTrue(repository.observeWordStats(child2Id).first().isEmpty())
        assertTrue(repository.observeExerciseStats(child2Id).first().isEmpty())

        repository.recordPracticeResults(
            childId = child2Id,
            exerciseType = ExerciseType.MATCHING,
            wordOutcomes = mapOf(words[0] to false),
            starsEarned = 1,
            score = 25
        )

        val profiles = repository.observeChildProfiles().first()
        assertEquals(2, profiles.size)
        assertEquals(3L, profiles.first { it.id == VocabRepository.DEFAULT_CHILD_ID }.totalStars)
        assertEquals(1L, profiles.first { it.id == child2Id }.totalStars)

        assertEquals(1, repository.observeWordStats(VocabRepository.DEFAULT_CHILD_ID).first().size)
        assertEquals(1L, repository.observeWordStats(VocabRepository.DEFAULT_CHILD_ID).first()[0].correctCount)
        assertEquals(0L, repository.observeWordStats(child2Id).first()[0].correctCount)

        // Delete Child 2 switches back to Child 1
        repository.deleteChildProfile(child2Id)
        val remainingProfiles = repository.observeChildProfiles().first()
        assertEquals(1, remainingProfiles.size)
        assertEquals(VocabRepository.DEFAULT_CHILD_ID, repository.observeSettings().first().activeChildId)

        // Deleting word cleans progress cleanly
        repository.deleteWord(packId, wordId)
        assertTrue(repository.observeWordStats(VocabRepository.DEFAULT_CHILD_ID).first().isEmpty())
    }

    @Test
    fun `pack assignment restricts visibility per child and resets cleanly on child deletion`() = runBlocking {
        repository.ensureStarterContentIfNeeded()
        val child2Id = repository.createChildProfile(
            name = "Matěj",
            avatarEmoji = "🐼",
            colorIndex = 3L,
            activateImmediately = false
        )

        val packForChild2 = repository.createPack(
            title = "Matějova slovíčka",
            description = "",
            emoji = "🚀",
            colorIndex = 4L,
            enabledExercises = listOf(ExerciseType.QUIZ.id),
            assignedChildIds = listOf(child2Id)
        )
        val packForAll = repository.createPack(
            title = "Společná slovíčka",
            description = "",
            emoji = "🌈",
            colorIndex = 1L,
            enabledExercises = listOf(ExerciseType.MATCHING.id),
            assignedChildIds = emptyList()
        )

        val allPacks = repository.observePacks().first()
        val pChild2 = allPacks.first { it.id == packForChild2 }
        val pAll = allPacks.first { it.id == packForAll }

        assertFalse(pChild2.isAssignedTo(VocabRepository.DEFAULT_CHILD_ID))
        assertTrue(pChild2.isAssignedTo(child2Id))
        assertTrue(pAll.isAssignedTo(VocabRepository.DEFAULT_CHILD_ID))
        assertTrue(pAll.isAssignedTo(child2Id))

        repository.deleteChildProfile(child2Id)
        val afterDelete = repository.observePacks().first().first { it.id == packForChild2 }
        assertTrue(afterDelete.assignedChildIds.isEmpty())
        assertTrue(afterDelete.isAssignedTo(VocabRepository.DEFAULT_CHILD_ID))
    }

    @Test
    fun `legacy Kamarade default profile upgrades to Dite 1 and Pexeso skips error and accuracy tracking`() = runBlocking {
        val now = System.currentTimeMillis()
        val dao = database.vocabDao()
        dao.upsertSettings(
            com.example.data.local.ParentSettingsEntity(
                id = "parent",
                childName = "Kamaráde",
                parentPinEnabled = false,
                parentPin = "",
                totalStars = 5L,
                createdAtMillis = now,
                updatedAtMillis = now,
                curriculumVersion = 4,
                activeChildId = VocabRepository.DEFAULT_CHILD_ID
            )
        )
        dao.upsertChildProfile(
            com.example.data.local.ChildProfileEntity(
                id = VocabRepository.DEFAULT_CHILD_ID,
                name = "Kamaráde",
                avatarEmoji = "🦁",
                colorIndex = 0L,
                totalStars = 5L,
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )

        repository.ensureStarterContentIfNeeded()

        val upgradedProfile = repository.observeChildProfiles().first().first()
        assertEquals("Dítě 1", upgradedProfile.name)
        assertEquals("Dítě 1", repository.observeSettings().first().childName)

        val unit1 = repository.observePacks().first().first { it.title == "Unit 1" }
        val words = repository.observeWords(unit1.id).first().take(2)
        repository.recordPracticeResults(
            childId = VocabRepository.DEFAULT_CHILD_ID,
            exerciseType = ExerciseType.MEMORY,
            wordOutcomes = words.associateWith { true },
            starsEarned = 3,
            score = 60
        )

        val exStats = repository.observeExerciseStats(VocabRepository.DEFAULT_CHILD_ID).first()
            .first { it.exerciseType == ExerciseType.MEMORY.id }
        assertEquals(1L, exStats.sessionsCompleted)
        assertEquals(0L, exStats.attempts)
        assertEquals(0L, exStats.correctCount)
        assertTrue(repository.observeWordStats(VocabRepository.DEFAULT_CHILD_ID).first().isEmpty())
        assertEquals(8L, repository.observeChildProfiles().first().first().totalStars)
    }
}
