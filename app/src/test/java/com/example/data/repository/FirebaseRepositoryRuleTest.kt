package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.FirestoreSavedRecord
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test

class FirebaseRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveRecord_authenticatedOwner_succeeds() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = FirebaseRepository(firestore)
        
        val record = FirestoreSavedRecord(
            id = "record_123",
            mouzaId = "mouza_abc",
            mouzaNameEn = "Alice Mouza"
        )
        
        repository.saveRecord(record)
        
        val records = withTimeout(5000) {
            repository.observeSavedRecords().first()
        }
        
        assertThat(records).isNotEmpty()
        assertThat(records.first().id).isEqualTo("record_123")
    }

    @Test
    fun observeSavedRecords_crossUserAccess_isolationMaintained() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = FirebaseRepository(firestore)
        aliceRepo.saveRecord(FirestoreSavedRecord(id = "alice_rec", mouzaNameEn = "Alice Mouza"))
        
        signInTestUser(BOB_EMAIL)
        val bobRepo = FirebaseRepository(firestore)
        val bobRecords = withTimeout(5000) {
            bobRepo.observeSavedRecords().first()
        }
        
        assertThat(bobRecords).isEmpty()
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val BOB_EMAIL = "bob@test.com"
    }
}
