package com.example.data.ledger

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class DocumentVerificationStatus(val labelEn: String, val labelBn: String) {
    CHECKSUM_VERIFIED("SHA-256 & PDF Verified", "SHA-256 ও PDF যাচাইকৃত"),
    USER_IMPORTED_PDF("User Imported Authentic PDF", "ব্যবহারকারী কর্তৃক ইমপোর্টকৃত PDF"),
    OCR_EXTRACTED_UNVERIFIED("OCR Extracted (Unverified)", "OCR হতে প্রাপ্ত (অযাচাইকৃত)"),
    USER_CORRECTED_OCR("User Corrected Metadata", "ব্যবহারকারী কর্তৃক সংশোধিত"),
    FAILED_OR_CORRUPTED("Failed / Corrupted File", "ত্রুটিপূর্ণ বা অসম্পূর্ণ ফাইল")
}

/**
 * Master Land Ledger entity organizing actual downloaded and user-imported land records (Section 9).
 * Never invents missing ownership information or treats raw OCR output as verified facts.
 */
@Entity(
    tableName = "master_land_ledger",
    indices = [
        Index(value = ["division_id"]),
        Index(value = ["district_id"]),
        Index(value = ["upazila_id"]),
        Index(value = ["mouza_id"]),
        Index(value = ["jl_number"]),
        Index(value = ["survey_type"]),
        Index(value = ["sha256_checksum"])
    ]
)
data class MasterLedgerEntryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "division_id") val divisionId: String,
    @ColumnInfo(name = "division_name") val divisionName: String,
    @ColumnInfo(name = "district_id") val districtId: String,
    @ColumnInfo(name = "district_name") val districtName: String,
    @ColumnInfo(name = "upazila_id") val upazilaId: String,
    @ColumnInfo(name = "upazila_name") val upazilaName: String,
    @ColumnInfo(name = "mouza_id") val mouzaId: String,
    @ColumnInfo(name = "mouza_name") val mouzaName: String,
    @ColumnInfo(name = "jl_number") val jlNumber: String,
    @ColumnInfo(name = "survey_type") val surveyType: String,
    @ColumnInfo(name = "khatian_number") val khatianNumber: String,
    @ColumnInfo(name = "dag_number") val dagNumber: String,
    @ColumnInfo(name = "owner_name") val ownerName: String,
    @ColumnInfo(name = "guardian_name") val guardianName: String,
    @ColumnInfo(name = "document_type") val documentType: String,
    @ColumnInfo(name = "source_name") val sourceName: String,
    @ColumnInfo(name = "retrieval_method") val retrievalMethod: String,
    @ColumnInfo(name = "verification_status") val verificationStatus: DocumentVerificationStatus,
    @ColumnInfo(name = "file_reference") val fileReference: String,
    @ColumnInfo(name = "file_size_bytes") val fileSizeBytes: Long,
    @ColumnInfo(name = "sha256_checksum") val sha256Checksum: String,
    @ColumnInfo(name = "original_ocr_text") val originalOcrText: String = "",
    @ColumnInfo(name = "user_corrected_notes") val userCorrectedNotes: String = "",
    @ColumnInfo(name = "recorded_at") val recordedAt: Long = System.currentTimeMillis()
)
