package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.geography.DistrictEntity
import com.example.data.geography.DivisionEntity
import com.example.data.geography.GeographyDao
import com.example.data.geography.GeographyDataSourceEntity
import com.example.data.geography.GeographyImportHistoryEntity
import com.example.data.geography.MouzaAliasEntity
import com.example.data.geography.MouzaEntity
import com.example.data.geography.MouzaJlNumberEntity
import com.example.data.geography.MouzaSurveyAvailabilityEntity
import com.example.data.geography.UpazilaEntity
import com.example.data.ledger.MasterLandLedgerDao
import com.example.data.ledger.MasterLedgerEntryEntity
import com.example.data.model.DownloadTask
import com.example.data.model.KhatianRecord
import com.example.data.model.SavedRecord
import com.example.data.model.UpazilaFetchInfo

@Database(
    entities = [
        SavedRecord::class,
        DownloadTask::class,
        KhatianRecord::class,
        DivisionEntity::class,
        DistrictEntity::class,
        UpazilaEntity::class,
        MouzaEntity::class,
        MouzaAliasEntity::class,
        MouzaJlNumberEntity::class,
        MouzaSurveyAvailabilityEntity::class,
        GeographyDataSourceEntity::class,
        GeographyImportHistoryEntity::class,
        MasterLedgerEntryEntity::class,
        UpazilaFetchInfo::class
    ],
    version = 8,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedRecordDao(): SavedRecordDao
    abstract fun downloadQueueDao(): DownloadQueueDao
    abstract fun khatianRecordDao(): KhatianRecordDao
    abstract fun geographyDao(): GeographyDao
    abstract fun masterLandLedgerDao(): MasterLandLedgerDao
    abstract fun upazilaFetchInfoDao(): UpazilaFetchInfoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private fun safeExec(db: SupportSQLiteDatabase, sql: String) {
            try {
                db.execSQL(sql)
            } catch (_: Exception) {
                // Column or table already exists
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                safeExec(
                    db,
                    """
                    CREATE TABLE IF NOT EXISTS `upazila_fetch_info` (
                        `upazilaId` TEXT NOT NULL,
                        `upazilaNameBn` TEXT NOT NULL,
                        `upazilaNameEn` TEXT NOT NULL,
                        `districtNameBn` TEXT NOT NULL,
                        `districtNameEn` TEXT NOT NULL DEFAULT '',
                        `fetchedMouzasCount` INTEGER NOT NULL,
                        `totalQueuesCreated` INTEGER NOT NULL,
                        `fetchTimestamp` INTEGER NOT NULL,
                        `message` TEXT NOT NULL,
                        `totalLandRecordsCount` INTEGER NOT NULL,
                        `mouzasPerQueue` INTEGER NOT NULL,
                        `networkProfileLabel` TEXT NOT NULL,
                        `deviceProfileLabel` TEXT NOT NULL,
                        `divisionId` TEXT NOT NULL DEFAULT 'chattogram',
                        `districtId` TEXT NOT NULL DEFAULT 'cumilla',
                        `selectedUpazilaIdsCsv` TEXT NOT NULL DEFAULT '',
                        `selectedMouzaIdsCsv` TEXT NOT NULL DEFAULT '',
                        `datasetVersion` TEXT NOT NULL DEFAULT '2024.11-BD64',
                        `activeBatchNumber` INTEGER NOT NULL DEFAULT 1,
                        `isQueuePaused` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`upazilaId`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Ensure upazila_fetch_info table and district-aware columns exist
                MIGRATION_6_7.migrate(db)
                safeExec(db, "ALTER TABLE `upazila_fetch_info` ADD COLUMN `districtNameEn` TEXT NOT NULL DEFAULT ''")
                safeExec(db, "ALTER TABLE `upazila_fetch_info` ADD COLUMN `divisionId` TEXT NOT NULL DEFAULT 'chattogram'")
                safeExec(db, "ALTER TABLE `upazila_fetch_info` ADD COLUMN `districtId` TEXT NOT NULL DEFAULT 'cumilla'")
                safeExec(db, "ALTER TABLE `upazila_fetch_info` ADD COLUMN `selectedUpazilaIdsCsv` TEXT NOT NULL DEFAULT ''")
                safeExec(db, "ALTER TABLE `upazila_fetch_info` ADD COLUMN `selectedMouzaIdsCsv` TEXT NOT NULL DEFAULT ''")
                safeExec(db, "ALTER TABLE `upazila_fetch_info` ADD COLUMN `datasetVersion` TEXT NOT NULL DEFAULT '2024.11-BD64'")
                safeExec(db, "ALTER TABLE `upazila_fetch_info` ADD COLUMN `activeBatchNumber` INTEGER NOT NULL DEFAULT 1")
                safeExec(db, "ALTER TABLE `upazila_fetch_info` ADD COLUMN `isQueuePaused` INTEGER NOT NULL DEFAULT 0")

                // 2. Ensure provenance columns exist on divisions, districts, upazilas, and mouzas
                listOf("divisions", "districts", "upazilas", "mouzas").forEach { table ->
                    safeExec(db, "ALTER TABLE `$table` ADD COLUMN `source_name` TEXT NOT NULL DEFAULT 'Bangladesh Bureau of Statistics (BBS) GeoCode & eporcha.tech Free Land Records Portal'")
                    safeExec(db, "ALTER TABLE `$table` ADD COLUMN `source_url` TEXT NOT NULL DEFAULT 'https://eporcha.tech/'")
                    safeExec(db, "ALTER TABLE `$table` ADD COLUMN `dataset_version` TEXT NOT NULL DEFAULT '2024.11-BD64'")
                    safeExec(db, "ALTER TABLE `$table` ADD COLUMN `imported_at` INTEGER NOT NULL DEFAULT 1730419200000")
                    safeExec(db, "ALTER TABLE `$table` ADD COLUMN `source_reference` TEXT NOT NULL DEFAULT 'BBS GeoCode / DLR&S Registry'")
                    safeExec(db, "ALTER TABLE `$table` ADD COLUMN `verification_status` TEXT NOT NULL DEFAULT 'VERIFIED'")
                }
                safeExec(db, "ALTER TABLE `mouzas` ADD COLUMN `official_code` TEXT NOT NULL DEFAULT ''")

                // 3. Reconcile legacy "adarsha-sadar" identifier into canonical "cumilla-adarsha-sadar"
                safeExec(db, "UPDATE `mouzas` SET `upazila_id` = 'cumilla-adarsha-sadar' WHERE `upazila_id` = 'adarsha-sadar'")
                safeExec(db, "DELETE FROM `upazilas` WHERE `id` = 'adarsha-sadar'")
                safeExec(db, "UPDATE `download_queue` SET `upazilaId` = 'cumilla-adarsha-sadar' WHERE `upazilaId` = 'adarsha-sadar'")
                safeExec(db, "UPDATE `upazila_fetch_info` SET `upazilaId` = 'cumilla-adarsha-sadar' WHERE `upazilaId` = 'adarsha-sadar'")
            }
        }

        val ALL_MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_6_7,
            MIGRATION_7_8
        )

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lr_downloader_database"
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
