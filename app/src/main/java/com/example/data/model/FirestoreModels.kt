package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class FirestoreUser(
    @DocumentId val userId: String = "",
    val displayName: String? = null,
    val email: String = "",
    val photoUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val lastSignInAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreSavedRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mouzaId: String = "",
    val mouzaNameBn: String = "",
    val mouzaNameEn: String = "",
    val jlNo: String = "",
    val upazilaNameBn: String = "",
    val districtNameBn: String = "",
    val recordType: String = "",
    val downloadFormat: String = "",
    val khatianCount: Int = 0,
    val fileSizeFormatted: String = "",
    @ServerTimestamp val downloadedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FirestoreDownloadTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mouzaId: String = "",
    val mouzaNameBn: String = "",
    val mouzaNameEn: String = "",
    val jlNo: String = "",
    val upazilaNameBn: String = "",
    val districtNameBn: String = "",
    val recordTypes: List<String> = emptyList(),
    val downloadFormat: String = "",
    val status: String = "PENDING",
    val progress: Int = 0,
    val totalKhatians: Int = 0,
    val downloadedKhatians: Int = 0,
    val fileSizeBytes: Long = 0,
    val speedKbps: Int = 0,
    val batchNumber: Int = 0,
    @ServerTimestamp val timestamp: Timestamp? = null,
    val referenceId: String? = null
)
