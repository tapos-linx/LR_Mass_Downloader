package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.geography.GeoVerificationStatus
import com.example.data.geography.SurveyAvailabilityStatus
import com.example.data.ledger.DocumentVerificationStatus
import com.example.data.model.DownloadFormat
import com.example.data.model.RecordType
import com.example.data.model.RetrievalMode
import com.example.data.model.TaskStatus

class Converters {
    @TypeConverter
    fun fromRecordTypeSet(value: Set<RecordType>): String {
        return value.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toRecordTypeSet(value: String): Set<RecordType> {
        if (value.isEmpty()) return emptySet()
        return value.split(",")
            .mapNotNull { token -> runCatching { RecordType.valueOf(token.trim()) }.getOrNull() }
            .toSet()
    }

    @TypeConverter
    fun fromTaskStatus(value: TaskStatus): String = value.name

    @TypeConverter
    fun toTaskStatus(value: String): TaskStatus =
        runCatching { TaskStatus.valueOf(value) }.getOrDefault(TaskStatus.PENDING)

    @TypeConverter
    fun fromDownloadFormat(value: DownloadFormat): String = value.name

    @TypeConverter
    fun toDownloadFormat(value: String): DownloadFormat =
        runCatching { DownloadFormat.valueOf(value) }.getOrDefault(DownloadFormat.PDF)

    @TypeConverter
    fun fromRetrievalMode(value: RetrievalMode): String = value.name

    @TypeConverter
    fun toRetrievalMode(value: String): RetrievalMode =
        runCatching { RetrievalMode.valueOf(value) }.getOrDefault(RetrievalMode.MODE_A_AUTHORIZED_API)

    @TypeConverter
    fun fromGeoVerificationStatus(value: GeoVerificationStatus): String = value.name

    @TypeConverter
    fun toGeoVerificationStatus(value: String): GeoVerificationStatus =
        runCatching { GeoVerificationStatus.valueOf(value) }.getOrDefault(GeoVerificationStatus.VERIFIED)

    @TypeConverter
    fun fromSurveyAvailabilityStatus(value: SurveyAvailabilityStatus): String = value.name

    @TypeConverter
    fun toSurveyAvailabilityStatus(value: String): SurveyAvailabilityStatus =
        runCatching { SurveyAvailabilityStatus.valueOf(value) }.getOrDefault(SurveyAvailabilityStatus.UNKNOWN)

    @TypeConverter
    fun fromDocumentVerificationStatus(value: DocumentVerificationStatus): String = value.name

    @TypeConverter
    fun toDocumentVerificationStatus(value: String): DocumentVerificationStatus =
        runCatching { DocumentVerificationStatus.valueOf(value) }.getOrDefault(DocumentVerificationStatus.CHECKSUM_VERIFIED)
}
