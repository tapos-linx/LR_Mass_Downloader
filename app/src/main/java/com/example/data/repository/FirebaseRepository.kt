package com.example.data.repository

import com.example.data.model.FirestoreDownloadTask
import com.example.data.model.FirestoreSavedRecord
import com.example.data.model.FirestoreUser
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirebaseRepository(private val db: FirebaseFirestore) {
    private fun requireUserId(): String {
        return Firebase.auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- User Profile ---

    suspend fun syncUserProfile() {
        val user = Firebase.auth.currentUser ?: return
        val userDoc = db.collection("users").document(user.uid)
        val userData = FirestoreUser(
            userId = user.uid,
            displayName = user.displayName,
            email = user.email ?: "",
            photoUrl = user.photoUrl?.toString()
        )
        
        try {
            // Using set with merge to avoid overwriting createdAt if it exists
            userDoc.set(userData, SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userDoc.path)
        }
    }

    // --- Saved Records ---

    fun observeSavedRecords(): Flow<List<FirestoreSavedRecord>> {
        val uid = requireUserId()
        return db.collection("users").document(uid).collection("saved_records")
            .snapshots()
            .map { it.toObjects(FirestoreSavedRecord::class.java) }
    }

    suspend fun saveRecord(record: FirestoreSavedRecord) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("saved_records").document(record.id)
        try {
            docRef.set(record.copy(userId = uid), SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
        }
    }

    suspend fun deleteRecord(recordId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("saved_records").document(recordId)
        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
        }
    }

    // --- Download Tasks ---

    fun observeDownloadTasks(): Flow<List<FirestoreDownloadTask>> {
        val uid = requireUserId()
        return db.collection("users").document(uid).collection("download_tasks")
            .snapshots()
            .map { it.toObjects(FirestoreDownloadTask::class.java) }
    }

    suspend fun updateDownloadTask(task: FirestoreDownloadTask) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("download_tasks").document(task.id)
        try {
            docRef.set(task.copy(userId = uid), SetOptions.merge()).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
        }
    }

    suspend fun deleteDownloadTask(taskId: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("download_tasks").document(taskId)
        try {
            docRef.delete().await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
        }
    }
}
