package com.example.data.geography

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Verification status for geographic entities and survey availability records.
 * Never claims an unverified or pending record is verified.
 */
enum class GeoVerificationStatus(val labelEn: String, val labelBn: String) {
    VERIFIED("Verified Official Record", "যাচাইকৃত সরকারি রেকর্ড"),
    CORROBORATED("Corroborated by Multiple Sources", "একাধিক সূত্রে যাচাইকৃত"),
    PROBABLE("Probable / High Confidence", "সম্ভাব্য"),
    POSSIBLE("Possible / Low Confidence", "সম্ভাব্য (কম নিশ্চিত)"),
    COMMUNITY_CONTRIBUTED("Community / Secondary Source", "তথ্যসূত্র সংযুক্ত (অযাচাইকৃত)"),
    UNVERIFIED("Unverified / Pending Audit", "অযাচাইকৃত"),
    RECORD_GAP("Known Record Gap", "রেকর্ড গ্যাপ"),
    CONTRADICTORY("Contradictory Records Found", "পরস্পরবিরোধী রেকর্ড"),
    MISSING_MOUZA_DATASET("Mouza Dataset Pending Import", "মৌজা ডেটাসেট সংযোজন বাকি")
}

/**
 * Availability status of a specific survey type in a Mouza.
 */
enum class SurveyAvailabilityStatus(val labelEn: String, val labelBn: String) {
    AVAILABLE("Available", "পাওয়া যাচ্ছে"),
    MANUAL_RETRIEVAL_REQUIRED("Manual Browser Retrieval Required", "ম্যানুয়াল ব্রাউজার ডাউনলোড প্রয়োজন"),
    UNKNOWN("Unknown Availability", "প্রাপ্যতা অজানা"),
    UNAVAILABLE("Not Conducted / Unavailable", "প্রযোজ্য নয় / অনুপলব্ধ")
}

/**
 * Survey and Record categories supported by LRgetBD (Section 6).
 */
enum class LandSurveyCategory(
    val code: String,
    val nameEn: String,
    val nameBn: String,
    val isSurveyType: Boolean = true
) {
    CS("CS", "Cadastral Survey (CS)", "সিএস (CS) জরিপ", true),
    SA("SA", "State Acquisition Survey (SA)", "এসএ (SA) জরিপ", true),
    RS("RS", "Revisional Survey (RS)", "আরএস (RS) জরিপ", true),
    BS_BRS("BS_BRS", "Bangladesh / City Survey (BS/BRS)", "বিএস / বিআরএস (BS/BRS) জরিপ", true),
    MUTATION("MUTATION", "Mutation / Namjari Khatian", "নামজারি / মিউটেশন খতিয়ান", true),
    KHATIAN("KHATIAN", "Individual Khatian", "খতিয়ান (Khatian)", false),
    DAG("DAG", "Dag / Plot Schedule", "দাগ সূচি (Dag)", false),
    MOUZA_MAP("MOUZA_MAP", "Mouza Map Sheet", "মৌজা ম্যাপ (Mouza Map)", false),
    MOUZA_KHATIAN_BOOK("MOUZA_KHATIAN_BOOK", "Mouza Khatian Book", "মৌজা খতিয়ান বহি", false),
    MOUZA_DAG_BOOK("MOUZA_DAG_BOOK", "Mouza Dag Book", "মৌজা দাগ বহি", false)
}

/**
 * 1. Normalized `divisions` table (All 8 Administrative Divisions of Bangladesh).
 */
@Entity(
    tableName = "divisions",
    indices = [
        Index(value = ["official_code"], unique = true),
        Index(value = ["name_en"])
    ]
)
data class DivisionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name_en") val nameEn: String,
    @ColumnInfo(name = "name_bn") val nameBn: String,
    @ColumnInfo(name = "official_code") val officialCode: String
)

/**
 * 2. Normalized `districts` table (All 64 Administrative Districts of Bangladesh).
 */
@Entity(
    tableName = "districts",
    indices = [
        Index(value = ["division_id"]),
        Index(value = ["official_code"], unique = true),
        Index(value = ["name_en"])
    ]
)
data class DistrictEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "division_id") val divisionId: String,
    @ColumnInfo(name = "name_en") val nameEn: String,
    @ColumnInfo(name = "name_bn") val nameBn: String,
    @ColumnInfo(name = "official_code") val officialCode: String
)

/**
 * 3. Normalized `upazilas` table.
 */
@Entity(
    tableName = "upazilas",
    indices = [
        Index(value = ["district_id"]),
        Index(value = ["official_code"]),
        Index(value = ["name_en"])
    ]
)
data class UpazilaEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "district_id") val districtId: String,
    @ColumnInfo(name = "name_en") val nameEn: String,
    @ColumnInfo(name = "name_bn") val nameBn: String,
    @ColumnInfo(name = "official_code") val officialCode: String
)

/**
 * 4. Normalized `mouzas` table.
 * A Mouza is scoped to its Upazila, District, and JL Number (never assumed globally unique by name).
 */
@Entity(
    tableName = "mouzas",
    indices = [
        Index(value = ["upazila_id"]),
        Index(value = ["jl_number"]),
        Index(value = ["upazila_id", "jl_number", "name_en"], unique = true),
        Index(value = ["verification_status"])
    ]
)
data class MouzaEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "upazila_id") val upazilaId: String,
    @ColumnInfo(name = "name_en") val nameEn: String,
    @ColumnInfo(name = "name_bn") val nameBn: String,
    @ColumnInfo(name = "jl_number") val jlNumber: String,
    @ColumnInfo(name = "official_mouza_code") val officialMouzaCode: String,
    @ColumnInfo(name = "source_reference") val sourceReference: String,
    @ColumnInfo(name = "source_version") val sourceVersion: String = "2024.11",
    @ColumnInfo(name = "source_url") val sourceUrl: String = "",
    @ColumnInfo(name = "verification_status") val verificationStatus: GeoVerificationStatus,
    @ColumnInfo(name = "last_verified_at") val lastVerifiedAt: Long
)

/**
 * 5. Normalized `mouza_aliases` table (supports alternative spellings, historical names, Bengali/English).
 */
@Entity(
    tableName = "mouza_aliases",
    indices = [
        Index(value = ["mouza_id"]),
        Index(value = ["alias_name"])
    ]
)
data class MouzaAliasEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "mouza_id") val mouzaId: String,
    @ColumnInfo(name = "alias_name") val aliasName: String,
    @ColumnInfo(name = "language") val language: String, // "bn", "en", "historical"
    @ColumnInfo(name = "source_reference") val sourceReference: String
)

/**
 * 6. Normalized `mouza_jl_numbers` table (supports survey-specific JL numbers per Mouza).
 */
@Entity(
    tableName = "mouza_jl_numbers",
    indices = [
        Index(value = ["mouza_id"]),
        Index(value = ["jl_number"]),
        Index(value = ["mouza_id", "survey_type"], unique = true)
    ]
)
data class MouzaJlNumberEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "mouza_id") val mouzaId: String,
    @ColumnInfo(name = "survey_type") val surveyType: String, // CS, SA, RS, BS_BRS
    @ColumnInfo(name = "jl_number") val jlNumber: String,
    @ColumnInfo(name = "source_reference") val sourceReference: String
)

/**
 * 7. Normalized `mouza_survey_availability` table.
 */
@Entity(
    tableName = "mouza_survey_availability",
    primaryKeys = ["mouza_id", "survey_type"],
    indices = [
        Index(value = ["mouza_id"]),
        Index(value = ["survey_type"]),
        Index(value = ["availability_status"])
    ]
)
data class MouzaSurveyAvailabilityEntity(
    @ColumnInfo(name = "mouza_id") val mouzaId: String,
    @ColumnInfo(name = "survey_type") val surveyType: String,
    @ColumnInfo(name = "availability_status") val availabilityStatus: SurveyAvailabilityStatus,
    @ColumnInfo(name = "source_reference") val sourceReference: String,
    @ColumnInfo(name = "last_checked_at") val lastCheckedAt: Long
)

/**
 * 8. Normalized `geography_data_sources` table.
 */
@Entity(
    tableName = "geography_data_sources",
    indices = [Index(value = ["source_name"], unique = true)]
)
data class GeographyDataSourceEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "source_name") val sourceName: String,
    @ColumnInfo(name = "source_url") val sourceUrl: String,
    @ColumnInfo(name = "dataset_version") val datasetVersion: String,
    @ColumnInfo(name = "verification_status") val verificationStatus: GeoVerificationStatus,
    @ColumnInfo(name = "record_count") val recordCount: Int,
    @ColumnInfo(name = "imported_at") val importedAt: Long
)

/**
 * 9. Normalized `geography_import_history` table.
 */
@Entity(
    tableName = "geography_import_history",
    indices = [Index(value = ["import_date"])]
)
data class GeographyImportHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "source_name") val sourceName: String,
    @ColumnInfo(name = "source_url") val sourceUrl: String,
    @ColumnInfo(name = "import_date") val importDate: Long,
    @ColumnInfo(name = "dataset_version") val datasetVersion: String,
    @ColumnInfo(name = "verification_status") val verificationStatus: GeoVerificationStatus,
    @ColumnInfo(name = "record_count") val recordCount: Int,
    @ColumnInfo(name = "import_errors") val importErrors: String
)

/**
 * Summary statistics of the local geographical database and current user selection.
 */
data class GeographyCoverageSummary(
    val totalDivisions: Int = 0,
    val totalDistricts: Int = 0,
    val totalUpazilas: Int = 0,
    val totalVerifiedMouzas: Int = 0,
    val totalUnverifiedMouzas: Int = 0,
    val upazilasMissingMouzaDataset: Int = 0,
    val selectedDistrictsCount: Int = 0,
    val selectedUpazilasCount: Int = 0,
    val totalMouzasFoundInSelection: Int = 0,
    val selectedMouzasCount: Int = 0,
    val mouzasWithAvailableSurveys: Int = 0,
    val mouzasWithUnknownAvailability: Int = 0,
    val mouzasRequiringManualRetrieval: Int = 0,
    val estimatedDownloadTasksCount: Int = 0
)
