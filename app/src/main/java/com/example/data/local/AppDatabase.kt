package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedRecordDao {
    @Query("SELECT * FROM saved_land_records ORDER BY downloadedAt DESC")
    fun getAllSavedRecords(): Flow<List<SavedRecord>>

    @Query("SELECT * FROM saved_land_records ORDER BY downloadedAt DESC")
    suspend fun getAllSavedRecordsDirect(): List<SavedRecord>

    @Query("SELECT * FROM saved_land_records WHERE sha256Checksum = :sha256 AND sha256Checksum != '' LIMIT 1")
    suspend fun findBySha256(sha256: String): SavedRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: SavedRecord)

    @Query("DELETE FROM saved_land_records WHERE id = :id")
    suspend fun deleteRecordById(id: String)

    @Query("DELETE FROM saved_land_records")
    suspend fun clearAllRecords()
}


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
        com.example.data.model.UpazilaFetchInfo::class
    ],
    version = 7,
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

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Phase 2: Add upazila_fetch_info table for persistent queue state recovery
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `upazila_fetch_info` (
                        `upazilaId` TEXT NOT NULL, 
                        `upazilaNameBn` TEXT NOT NULL, 
                        `upazilaNameEn` TEXT NOT NULL, 
                        `districtNameBn` TEXT NOT NULL, 
                        `totalMouzaCount` INTEGER NOT NULL, 
                        `batchCount` INTEGER NOT NULL, 
                        `message` TEXT NOT NULL, 
                        `totalLandRecordsCount` INTEGER NOT NULL, 
                        `mouzasPerQueue` INTEGER NOT NULL, 
                        `networkProfileLabel` TEXT NOT NULL, 
                        `deviceProfileLabel` TEXT NOT NULL, 
                        PRIMARY KEY(`upazilaId`)
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lr_mass_downloader.db"
                )
                    .addMigrations(MIGRATION_6_7)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getTestDatabase(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
        }
    }
}
