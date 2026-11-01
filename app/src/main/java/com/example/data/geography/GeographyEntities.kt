package com.example.data.geography

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Verification statuses for geographic records in the LRgetBD database.
 * Ensures the app never silently mixes unverified or placeholder records with
 * verified BBS/DLR&S gazetteer records.
 */
object GeoVerificationStatus {
    const val VERIFIED = "VERIFIED"
    const val COMMUNITY_CONTRIBUTED = "COMMUNITY_CONTRIBUTED"
    const val UNVERIFIED = "UNVERIFIED"
}

/**
 * Survey availability states for each Mouza & Survey Type (CS, SA, RS, BS/BRS, Diara, Mutation).
 */
object SurveyAvailabilityStatus {
    const val AVAILABLE = "AVAILABLE"
    const val MANUAL_RETRIEVAL_REQUIRED = "MANUAL_RETRIEVAL_REQUIRED"
    const val UNKNOWN = "UNKNOWN"
    const val UNAVAILABLE = "UNAVAILABLE"
}

@Entity(
    tableName = "divisions",
    indices = [
        Index(value = ["official_code"]),
        Index(value = ["name_en"]),
        Index(value = ["name_bn"])
    ]
)
data class DivisionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "name_en")
    val nameEn: String,
    @ColumnInfo(name = "name_bn")
    val nameBn: String,
    @ColumnInfo(name = "official_code")
    val officialCode: String,
    @ColumnInfo(name = "source_name")
    val sourceName: String = "Bangladesh Bureau of Statistics (BBS) GeoCode & DLR&S Gazetteer",
    @ColumnInfo(name = "source_url")
    val sourceUrl: String = "https://eporcha.tech/",
    @ColumnInfo(name = "dataset_version")
    val datasetVersion: String = "2024.11-BD64",
    @ColumnInfo(name = "imported_at")
    val importedAt: Long = 1730419200000L,
    @ColumnInfo(name = "source_reference")
    val sourceReference: String = "BBS GeoCode / Bangladesh National Portal",
    @ColumnInfo(name = "verification_status")
    val verificationStatus: String = GeoVerificationStatus.VERIFIED
)

@Entity(
    tableName = "districts",
    foreignKeys = [
        ForeignKey(
            entity = DivisionEntity::class,
            parentColumns = ["id"],
            childColumns = ["division_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["division_id"]),
        Index(value = ["official_code"]),
        Index(value = ["name_en"]),
        Index(value = ["name_bn"])
    ]
)
data class DistrictEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "division_id")
    val divisionId: String,
    @ColumnInfo(name = "name_en")
    val nameEn: String,
    @ColumnInfo(name = "name_bn")
    val nameBn: String,
    @ColumnInfo(name = "official_code")
    val officialCode: String,
    @ColumnInfo(name = "source_name")
    val sourceName: String = "Bangladesh Bureau of Statistics (BBS) GeoCode & DLR&S Gazetteer",
    @ColumnInfo(name = "source_url")
    val sourceUrl: String = "https://eporcha.tech/",
    @ColumnInfo(name = "dataset_version")
    val datasetVersion: String = "2024.11-BD64",
    @ColumnInfo(name = "imported_at")
    val importedAt: Long = 1730419200000L,
    @ColumnInfo(name = "source_reference")
    val sourceReference: String = "BBS GeoCode / DLR&S District Registry",
    @ColumnInfo(name = "verification_status")
    val verificationStatus: String = GeoVerificationStatus.VERIFIED
)

@Entity(
    tableName = "upazilas",
    foreignKeys = [
        ForeignKey(
            entity = DistrictEntity::class,
            parentColumns = ["id"],
            childColumns = ["district_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["district_id"]),
        Index(value = ["official_code"]),
        Index(value = ["name_en"]),
        Index(value = ["name_bn"])
    ]
)
data class UpazilaEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "district_id")
    val districtId: String,
    @ColumnInfo(name = "name_en")
    val nameEn: String,
    @ColumnInfo(name = "name_bn")
    val nameBn: String,
    @ColumnInfo(name = "official_code")
    val officialCode: String,
    @ColumnInfo(name = "source_name")
    val sourceName: String = "Bangladesh Bureau of Statistics (BBS) GeoCode & DLR&S Gazetteer",
    @ColumnInfo(name = "source_url")
    val sourceUrl: String = "https://eporcha.tech/",
    @ColumnInfo(name = "dataset_version")
    val datasetVersion: String = "2024.11-BD64",
    @ColumnInfo(name = "imported_at")
    val importedAt: Long = 1730419200000L,
    @ColumnInfo(name = "source_reference")
    val sourceReference: String = "BBS GeoCode / DLR&S Upazila Registry",
    @ColumnInfo(name = "verification_status")
    val verificationStatus: String = GeoVerificationStatus.VERIFIED
)

@Entity(
    tableName = "mouzas",
    foreignKeys = [
        ForeignKey(
            entity = UpazilaEntity::class,
            parentColumns = ["id"],
            childColumns = ["upazila_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["upazila_id"]),
        Index(value = ["jl_number"]),
        Index(value = ["name_en"]),
        Index(value = ["name_bn"]),
        Index(value = ["upazila_id", "jl_number", "name_en"], unique = true)
    ]
)
data class MouzaEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "upazila_id")
    val upazilaId: String,
    @ColumnInfo(name = "name_en")
    val nameEn: String,
    @ColumnInfo(name = "name_bn")
    val nameBn: String,
    @ColumnInfo(name = "jl_number")
    val jlNumber: String,
    @ColumnInfo(name = "official_mouza_code")
    val officialMouzaCode: String = "",
    @ColumnInfo(name = "official_code")
    val officialCode: String = officialMouzaCode,
    @ColumnInfo(name = "source_name")
    val sourceName: String = "Bangladesh Bureau of Statistics (BBS) GeoCode & DLR&S Gazetteer",
    @ColumnInfo(name = "source_url")
    val sourceUrl: String = "https://eporcha.tech/",
    @ColumnInfo(name = "dataset_version")
    val datasetVersion: String = "2024.11-BD64",
    @ColumnInfo(name = "imported_at")
    val importedAt: Long = 1730419200000L,
    @ColumnInfo(name = "source_reference")
    val sourceReference: String = "DLR&S Mouza Gazetteer",
    @ColumnInfo(name = "verification_status")
    val verificationStatus: String = GeoVerificationStatus.VERIFIED,
    @ColumnInfo(name = "last_verified_at")
    val lastVerifiedAt: Long = 1730419200000L
)

@Entity(
    tableName = "mouza_aliases",
    foreignKeys = [
        ForeignKey(
            entity = MouzaEntity::class,
            parentColumns = ["id"],
            childColumns = ["mouza_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["mouza_id"]),
        Index(value = ["alias_name"])
    ]
)
data class MouzaAliasEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,
    @ColumnInfo(name = "mouza_id")
    val mouzaId: String,
    @ColumnInfo(name = "alias_name")
    val aliasName: String,
    @ColumnInfo(name = "language")
    val language: String = "bn",
    @ColumnInfo(name = "source_reference")
    val sourceReference: String = "Historical / Regional Gazetteer Spelling"
)

@Entity(
    tableName = "mouza_jl_numbers",
    foreignKeys = [
        ForeignKey(
            entity = MouzaEntity::class,
            parentColumns = ["id"],
            childColumns = ["mouza_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["mouza_id"]),
        Index(value = ["survey_type"]),
        Index(value = ["jl_number"])
    ]
)
data class MouzaJlNumberEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,
    @ColumnInfo(name = "mouza_id")
    val mouzaId: String,
    @ColumnInfo(name = "survey_type")
    val surveyType: String,
    @ColumnInfo(name = "jl_number")
    val jlNumber: String,
    @ColumnInfo(name = "source_reference")
    val sourceReference: String = "DLR&S Survey Register"
)

@Entity(
    tableName = "mouza_survey_availability",
    foreignKeys = [
        ForeignKey(
            entity = MouzaEntity::class,
            parentColumns = ["id"],
            childColumns = ["mouza_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["mouza_id"]),
        Index(value = ["survey_type"]),
        Index(value = ["mouza_id", "survey_type"], unique = true)
    ]
)
data class MouzaSurveyAvailabilityEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,
    @ColumnInfo(name = "mouza_id")
    val mouzaId: String,
    @ColumnInfo(name = "survey_type")
    val surveyType: String,
    @ColumnInfo(name = "availability_status")
    val availabilityStatus: String = SurveyAvailabilityStatus.AVAILABLE,
    @ColumnInfo(name = "source_reference")
    val sourceReference: String = "DLR&S Survey Register",
    @ColumnInfo(name = "last_checked_at")
    val lastCheckedAt: Long = 1730419200000L
)

@Entity(tableName = "geography_data_sources")
data class GeographyDataSourceEntity(
    @PrimaryKey
    @ColumnInfo(name = "source_id")
    val sourceId: String,
    @ColumnInfo(name = "source_name")
    val sourceName: String,
    @ColumnInfo(name = "source_url")
    val sourceUrl: String,
    @ColumnInfo(name = "retrieval_date")
    val retrievalDate: Long,
    @ColumnInfo(name = "dataset_version")
    val datasetVersion: String,
    @ColumnInfo(name = "record_count")
    val recordCount: Int
)

@Entity(tableName = "geography_import_history")
data class GeographyImportHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "import_id")
    val importId: Long = 0L,
    @ColumnInfo(name = "source_name")
    val sourceName: String,
    @ColumnInfo(name = "source_url")
    val sourceUrl: String,
    @ColumnInfo(name = "imported_at")
    val importedAt: Long,
    @ColumnInfo(name = "dataset_version")
    val datasetVersion: String,
    @ColumnInfo(name = "record_count")
    val recordCount: Int,
    @ColumnInfo(name = "import_errors")
    val importErrors: String = ""
)

/**
 * Aggregated real-time coverage summary computed directly from Room tables.
 * Explicitly reports incomplete district coverage when not all upazilas in a district
 * have complete verified mouza inventories.
 */
data class GeographyCoverageSummary(
    val totalDivisions: Int = 0,
    val totalDistricts: Int = 0,
    val totalUpazilas: Int = 0,
    val totalMouzas: Int = 0,
    val totalVerifiedMouzas: Int = 0,
    val totalUnverifiedMouzas: Int = 0,
    val selectedDistrictsCount: Int = 0,
    val selectedUpazilasCount: Int = 0,
    val totalMouzasFoundInSelection: Int = 0,
    val selectedMouzasCount: Int = 0,
    val mouzasWithAvailableSurveys: Int = 0,
    val mouzasWithUnknownAvailability: Int = 0,
    val mouzasRequiringManualRetrieval: Int = 0,
    val isCurrentDistrictComplete: Boolean = false,
    val incompleteDataWarning: String = ""
)

typealias VerificationStatus = GeoVerificationStatus

