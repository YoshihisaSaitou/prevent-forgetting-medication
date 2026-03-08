package com.example.preventforgettingmedicationandroidapp

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Medication::class,
        Schedule::class,
        ScheduleMedicationCrossRef::class,
        IntakeHistory::class
    ],
    version = 8
)
@TypeConverters(Converters::class)
abstract class MedicationDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun intakeHistoryDao(): IntakeHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: MedicationDatabase? = null

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS schedules_seed (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        slot TEXT NOT NULL,
                        timeMinutes INTEGER NOT NULL,
                        isActive INTEGER NOT NULL,
                        legacyMedicationId INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO schedules_seed(name, slot, timeMinutes, isActive, legacyMedicationId)
                    SELECT name || ' - Morning', 'MORNING',
                           COALESCE(CASE WHEN useAppTimes = 0 THEN morningMinutes END, 420),
                           1,
                           id
                    FROM medications
                    WHERE timing LIKE '%MORNING%'
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO schedules_seed(name, slot, timeMinutes, isActive, legacyMedicationId)
                    SELECT name || ' - Noon', 'NOON',
                           COALESCE(CASE WHEN useAppTimes = 0 THEN noonMinutes END, 720),
                           1,
                           id
                    FROM medications
                    WHERE timing LIKE '%NOON%'
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO schedules_seed(name, slot, timeMinutes, isActive, legacyMedicationId)
                    SELECT name || ' - Evening', 'EVENING',
                           COALESCE(CASE WHEN useAppTimes = 0 THEN eveningMinutes END, 1140),
                           1,
                           id
                    FROM medications
                    WHERE timing LIKE '%EVENING%'
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS schedules (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        slot TEXT NOT NULL,
                        timeMinutes INTEGER NOT NULL,
                        isActive INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO schedules(id, name, slot, timeMinutes, isActive)
                    SELECT id, name, slot, timeMinutes, isActive
                    FROM schedules_seed
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS schedule_medications (
                        scheduleId INTEGER NOT NULL,
                        medicationId INTEGER NOT NULL,
                        displayOrder INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(scheduleId, medicationId),
                        FOREIGN KEY(scheduleId) REFERENCES schedules(id) ON DELETE CASCADE,
                        FOREIGN KEY(medicationId) REFERENCES medications(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    INSERT INTO schedule_medications(scheduleId, medicationId, displayOrder)
                    SELECT id, legacyMedicationId, 0
                    FROM schedules_seed
                    """.trimIndent()
                )

                db.execSQL("DROP TABLE schedules_seed")

                db.execSQL("ALTER TABLE intake_history ADD COLUMN scheduleId INTEGER")
                db.execSQL("ALTER TABLE intake_history ADD COLUMN scheduleName TEXT")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_schedule_medications_scheduleId ON schedule_medications(scheduleId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_schedule_medications_medicationId ON schedule_medications(medicationId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_intake_history_scheduleId_takenAt ON intake_history(scheduleId, takenAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_intake_history_medicationId_takenAt ON intake_history(medicationId, takenAt)")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Keep data and remove duplicate rows before enforcing the unique constraint.
                db.execSQL(
                    """
                    DELETE FROM intake_history
                    WHERE scheduleId IS NOT NULL
                      AND id NOT IN (
                        SELECT MIN(id)
                        FROM intake_history
                        WHERE scheduleId IS NOT NULL
                        GROUP BY scheduleId, medicationId, takenAt
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_intake_history_schedule_medication_takenAt_unique
                    ON intake_history(scheduleId, medicationId, takenAt)
                    WHERE scheduleId IS NOT NULL
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): MedicationDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MedicationDatabase::class.java,
                    "medications.db"
                )
                    .addMigrations(MIGRATION_6_7, MIGRATION_7_8)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}