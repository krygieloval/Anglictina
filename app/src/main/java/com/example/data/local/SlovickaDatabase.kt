package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ExerciseType

@Database(
    entities = [
        ChildProfileEntity::class,
        WordPackEntity::class,
        VocabWordEntity::class,
        WordProgressEntity::class,
        ExerciseTypeStatsEntity::class,
        ParentSettingsEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class SlovickaDatabase : RoomDatabase() {
    abstract fun vocabDao(): VocabDao

    companion object {
        @Volatile
        private var INSTANCE: SlovickaDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val allIds = ExerciseType.defaultIds.joinToString(",")
                db.execSQL(
                    "UPDATE word_packs SET enabledExercisesCsv = ? WHERE enabledExercisesCsv = ?",
                    arrayOf(allIds, "MATCHING,FILL_BLANK,PRONUNCIATION,SHOOTER")
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE parent_settings ADD COLUMN curriculumVersion INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `child_profiles` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `avatarEmoji` TEXT NOT NULL,
                        `colorIndex` INTEGER NOT NULL,
                        `totalStars` INTEGER NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "ALTER TABLE `parent_settings` ADD COLUMN `activeChildId` TEXT NOT NULL DEFAULT 'default_child'"
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `child_profiles` (`id`, `name`, `avatarEmoji`, `colorIndex`, `totalStars`, `createdAtMillis`, `updatedAtMillis`)
                    SELECT 'default_child', COALESCE(NULLIF(TRIM(`childName`), ''), 'Dítě 1'), '🦁', 0, `totalStars`, `createdAtMillis`, `updatedAtMillis`
                    FROM `parent_settings` WHERE `id` = 'parent'
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `child_profiles` (`id`, `name`, `avatarEmoji`, `colorIndex`, `totalStars`, `createdAtMillis`, `updatedAtMillis`)
                    VALUES ('default_child', 'Dítě 1', '🦁', 0, 0, 0, 0)
                    """.trimIndent()
                )

                db.execSQL(
                    "ALTER TABLE `word_progress` ADD COLUMN `childId` TEXT NOT NULL DEFAULT 'default_child'"
                )
                db.execSQL(
                    "ALTER TABLE `word_progress` ADD COLUMN `wordId` TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "UPDATE `word_progress` SET `wordId` = `id`, `id` = 'default_child_' || `id` WHERE `wordId` = ''"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_word_progress_childId` ON `word_progress` (`childId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_word_progress_wordId` ON `word_progress` (`wordId`)"
                )

                db.execSQL(
                    "ALTER TABLE `exercise_stats` ADD COLUMN `childId` TEXT NOT NULL DEFAULT 'default_child'"
                )
                db.execSQL(
                    "UPDATE `exercise_stats` SET `id` = 'default_child_' || `exerciseType` WHERE `id` NOT LIKE 'default_child_%'"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_exercise_stats_childId` ON `exercise_stats` (`childId`)"
                )

                db.execSQL("UPDATE `vocab_words` SET `exampleSentence` = ''")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `word_packs` ADD COLUMN `assignedChildIdsCsv` TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        fun getInstance(context: Context): SlovickaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SlovickaDatabase::class.java,
                    "slovicka.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
