package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.geography.BangladeshGeographyCatalog
import com.example.data.geography.GeoVerificationStatus
import com.example.data.ledger.DocumentVerificationStatus

enum class RecordType(val code: String, val titleBn: String, val titleEn: String, val yearRange: String) {
    CS("CS", "সিএস (CS)", "Cadastral Survey", "1888–1940"),
    SA("SA", "এসএ (SA)", "State Acquisition", "1956–1962"),
    RS("RS", "আরএস (RS)", "Revisional Survey", "1965–1990"),
    BS("BS", "বিএস/বিআরএস (BS)", "Bangladesh Survey", "1998–Present");

    val bengaliName: String
        get() = titleBn
}

enum class DownloadFormat(val code: String, val labelBn: String, val labelEn: String, val extension: String) {
    PDF_AND_JPG("PDF+JPG", "পিডিএফ ও জেপিজি (PDF + JPG)", "PDF & JPG Image", ".pdf/.jpg"),
    PDF("PDF", "শুধুমাত্র পিডিএফ (PDF)", "Official PDF Document", ".pdf"),
    PDF_ONLY("PDF", "শুধুমাত্র পিডিএফ (PDF)", "Official PDF Document", ".pdf"),
    JPG_ONLY("JPG", "শুধুমাত্র ছবি (JPG)", "High-Res Scan JPG", ".jpg"),
    JSON_LEDGER("JSON", "মাস্টার লেজার (JSON)", "Structured Archive JSON", ".json");

    val label: String
        get() = labelBn
}

/**
 * Dual-Mode Free Retrieval Architecture (Powered by https://eporcha.tech/ — 100% Free, Zero Billing Limitations):
 * - MODE_A_AUTHORIZED_API: Automated queue downloader connected to https://eporcha.tech/ (free open access).
 * - MODE_B_USER_ASSISTED: User-assisted https://eporcha.tech/ browser & genuine PDF document importer.
 */
enum class RetrievalMode(val labelBn: String, val labelEn: String) {
    MODE_A_AUTHORIZED_API("মোড A: eporcha.tech ফ্রি স্বয়ংক্রিয় কিউ", "Mode A: eporcha.tech Free Queue"),
    MODE_B_USER_ASSISTED("মোড B: eporcha.tech ব্রাউজার ও PDF ইমপোর্ট", "Mode B: eporcha.tech Browser & PDF Import")
}

enum class TaskStatus {
    QUEUED,
    PENDING,
    RUNNING,
    AUTHENTICATING,
    VERIFYING,
    PAUSED,
    WAITING_FOR_USER,
    AWAITING_USER_IMPORT,
    WAITING_FOR_NETWORK,
    IMPORTED,
    DOWNLOADING,
    VALIDATING,
    VERIFIED,
    COMPLETED,
    FAILED,
    UNAVAILABLE,
    CANCELLED
}

data class Mouza(
    val id: String,
    val nameBn: String,
    val nameEn: String,
    val jlNo: String,
    val baseKhatiansPerType: Int = 50,
    val upazilaId: String = "",
    val officialCode: String = "",
    val sourceName: String = BangladeshGeographyCatalog.OFFICIAL_SOURCE_NAME,
    val sourceUrl: String = BangladeshGeographyCatalog.OFFICIAL_SOURCE_URL,
    val datasetVersion: String = BangladeshGeographyCatalog.DATASET_VERSION,
    val importedAt: Long = BangladeshGeographyCatalog.DEFAULT_IMPORTED_AT,
    val sourceReference: String = "BBS / DLR&S Gazetteer",
    val verificationStatus: String = GeoVerificationStatus.VERIFIED
) {
    val estimatedKhatians: Int
        get() = baseKhatiansPerType * RecordType.values().size

    fun totalKhatiansFor(types: Set<RecordType>): Int {
        val count = if (types.isEmpty()) 1 else types.size
        return baseKhatiansPerType * count
    }
}

data class Upazila(
    val id: String,
    val nameBn: String,
    val nameEn: String,
    val mouzas: List<Mouza>,
    val districtId: String = "",
    val officialCode: String = "",
    val sourceName: String = BangladeshGeographyCatalog.OFFICIAL_SOURCE_NAME,
    val sourceUrl: String = BangladeshGeographyCatalog.OFFICIAL_SOURCE_URL,
    val datasetVersion: String = BangladeshGeographyCatalog.DATASET_VERSION,
    val importedAt: Long = BangladeshGeographyCatalog.DEFAULT_IMPORTED_AT,
    val sourceReference: String = "BBS / DLR&S Gazetteer",
    val verificationStatus: String = GeoVerificationStatus.VERIFIED
)

data class District(
    val id: String,
    val nameBn: String,
    val nameEn: String,
    val upazilas: List<Upazila>,
    val divisionId: String = "chattogram",
    val officialCode: String = "",
    val sourceName: String = BangladeshGeographyCatalog.OFFICIAL_SOURCE_NAME,
    val sourceUrl: String = BangladeshGeographyCatalog.OFFICIAL_SOURCE_URL,
    val datasetVersion: String = BangladeshGeographyCatalog.DATASET_VERSION,
    val importedAt: Long = BangladeshGeographyCatalog.DEFAULT_IMPORTED_AT,
    val sourceReference: String = "BBS / DLR&S Gazetteer",
    val verificationStatus: String = GeoVerificationStatus.VERIFIED
)

@Entity(
    tableName = "download_queue",
    indices = [
        Index(value = ["divisionId"]),
        Index(value = ["districtId"]),
        Index(value = ["upazilaId"]),
        Index(value = ["mouzaId"]),
        Index(value = ["jlNo"]),
        Index(value = ["surveyType"]),
        Index(value = ["status"]),
        Index(value = ["batchNumber", "sortOrder"])
    ]
)
data class DownloadTask(
    @PrimaryKey val id: String,
    val mouzaId: String,
    val mouzaNameBn: String,
    val mouzaNameEn: String,
    val jlNo: String,
    val upazilaNameBn: String,
    val districtNameBn: String,
    val recordTypes: Set<RecordType>,
    val downloadFormat: DownloadFormat = DownloadFormat.PDF,
    val status: TaskStatus = TaskStatus.PENDING,
    val progress: Int = 0,
    val totalKhatians: Int,
    val downloadedKhatians: Int = 0,
    val fileSizeBytes: Long = 0L,
    val speedKbps: Int = 0,
    val batchNumber: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val referenceId: String? = null,
    val divisionId: String = "chattogram",
    val divisionNameBn: String = "চট্টগ্রাম",
    val districtId: String = "cumilla",
    val upazilaId: String = "cumilla-adarsha-sadar",
    val surveyType: String = "RS",
    val recordCategory: String = "KHATIAN",
    val retrievalMode: RetrievalMode = RetrievalMode.MODE_A_AUTHORIZED_API,
    val sourceName: String = "eporcha.tech Free Land Records Portal",
    val sourceReference: String = "https://eporcha.tech/",
    val fileLocation: String = "",
    val sha256Checksum: String = "",
    val errorDetails: String? = null,
    val completedTimestamp: Long? = null,
    val sortOrder: Int = 0
) {
    val recordTypeLabel: String
        get() = recordTypes.joinToString(" + ") { it.code }
}

@Entity(tableName = "saved_land_records")
data class SavedRecord(
    @PrimaryKey val id: String,
    val mouzaId: String,
    val mouzaNameBn: String,
    val mouzaNameEn: String,
    val jlNo: String,
    val upazilaNameBn: String,
    val districtNameBn: String,
    val recordType: String,
    val khatianCount: Int,
    val fileSizeFormatted: String,
    val downloadedAt: Long = System.currentTimeMillis(),
    val divisionNameBn: String = "চট্টগ্রাম",
    val fileLocation: String = "",
    val sha256Checksum: String = "",
    val sourceName: String = "LRgetBD Verified Archive",
    val verificationStatus: String = DocumentVerificationStatus.CHECKSUM_VERIFIED.name
) {
    val timestamp: Long
        get() = downloadedAt
}

@Entity(tableName = "khatian_records")
data class KhatianRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val district: String,
    val upazila: String,
    val mouza: String,
    val khatianNumber: String,
    val recordType: String,
    val downloadStatus: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Consolidated Master Upazila Archive metadata once all batches finish.
 */
data class MasterArchive(
    val upazilaId: String,
    val upazilaNameBn: String,
    val upazilaNameEn: String,
    val districtNameBn: String,
    val districtNameEn: String,
    val totalMouzasCount: Int,
    val recordTypesIncluded: String,
    val totalKhatiansCount: Int,
    val totalSizeMb: String,
    val fileName: String,
    val isReady: Boolean = false,
    val isDownloadedToPhone: Boolean = false,
    val phoneSavedPath: String? = null,
    val textFileSavedPath: String? = null,
    val csvFileSavedPath: String? = null,
    val xlsxFileSavedPath: String? = null,
    val pdfFileSavedPath: String? = null,
    val sha256Checksum: String? = null,
    val savedFilesList: List<String> = emptyList(),
    val fileContentPreview: String? = null
)

/**
 * Represents one planned 7-Mouza queue segment (`Queue #1`, `Queue #2`, ..., `Queue #N`)
 * generated automatically when an Upazila or District is selected.
 */
data class PlannedQueueSegment(
    val queueNumber: Int,
    val mouzas: List<Mouza>,
    val totalLandRecordsCount: Int,
    val isCompleted: Boolean = false,
    val isActive: Boolean = false,
    val completedMouzasCount: Int = 0
) {
    val mouzasCount: Int get() = mouzas.size
    val mouzaCount: Int get() = mouzas.size
    val status: TaskStatus
        get() = when {
            isCompleted -> TaskStatus.COMPLETED
            isActive -> TaskStatus.DOWNLOADING
            else -> TaskStatus.PENDING
        }
    val progressPercent: Int
        get() = if (mouzas.isEmpty()) 0 else ((completedMouzasCount * 100) / mouzas.size).coerceIn(0, 100)
    val jlRangeLabel: String
        get() = if (mouzas.isEmpty()) "" else "JL ${mouzas.first().jlNo} – ${mouzas.last().jlNo}"
}

/**
 * District-aware persistent selection and queue recovery state (Phase 4).
 * Stores divisionId, districtId, selectedUpazilaIds, selectedMouzaIds, batch configuration,
 * dataset version, and queue recovery metadata.
 */
@Entity(tableName = "upazila_fetch_info")
data class UpazilaFetchInfo(
    @PrimaryKey val upazilaId: String,
    val upazilaNameBn: String = "",
    val upazilaNameEn: String = "",
    val districtNameBn: String = "",
    val districtNameEn: String = "",
    val fetchedMouzasCount: Int = 0,
    val totalQueuesCreated: Int = 0,
    val fetchTimestamp: Long = System.currentTimeMillis(),
    val message: String = "",
    val totalLandRecordsCount: Int = 0,
    val mouzasPerQueue: Int = 7,
    val networkProfileLabel: String = "High-Speed Network",
    val deviceProfileLabel: String = "Multi-Core Device",
    val divisionId: String = "chattogram",
    val districtId: String = "cumilla",
    val selectedUpazilaIdsCsv: String = upazilaId,
    val selectedMouzaIdsCsv: String = "",
    val datasetVersion: String = BangladeshGeographyCatalog.DATASET_VERSION,
    val activeBatchNumber: Int = 1,
    val isQueuePaused: Boolean = false
) {
    val totalMouzaCount: Int
        get() = fetchedMouzasCount

    val batchCount: Int
        get() = totalQueuesCreated

    val selectedUpazilaIdsSet: Set<String>
        get() = selectedUpazilaIdsCsv.split(",")
            .map { BangladeshGeographyCatalog.resolveCanonicalUpazilaId(it) }
            .filter { it.isNotEmpty() }
            .toSet()

    val selectedMouzaIdsSet: Set<String>
        get() = selectedMouzaIdsCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
}

object LandRecordsDataset {

    /**
     * Authoritative national dataset constructed directly from the single canonical
     * `BangladeshGeographyCatalog` hierarchy (8 Divisions, 64 Districts, all official Upazilas,
     * and verified Mouzas).
     */
    val districts: List<District> by lazy {
        BangladeshGeographyCatalog.buildCanonicalDistrictModels()
    }
}
