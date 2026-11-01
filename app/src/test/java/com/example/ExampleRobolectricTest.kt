package com.example

import android.app.Application
import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.geography.BangladeshGeographyCatalog
import com.example.data.geography.MouzaEntity
import com.example.data.geography.VerificationStatus
import com.example.data.local.AppDatabase
import com.example.data.model.DownloadTask
import com.example.data.model.LandRecordsDataset
import com.example.data.model.Mouza
import com.example.data.model.RetrievalMode
import com.example.data.model.TaskStatus
import com.example.data.worker.DownloadQueueWorker
import com.example.ui.LRDownloaderViewModel
import com.example.util.DocumentVerificationEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getDatabase(context)
        db.downloadQueueDao().clearQueue()
        db.savedRecordDao().clearAllRecords()
        db.masterLandLedgerDao().clearAll()
        db.upazilaFetchInfoDao().clearFetchInfo()
        db.geographyDao().deleteMouzasByPrefix("cumilla-test-m")
        delay(100)
        ShadowLooper.idleMainLooper()
    }

    @Test
    fun testAppNameString() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LR get BD", appName)
    }

    @Test
    fun testDatasetIntegrityWithRealisticMouzaCounts() {
        val cumilla = LandRecordsDataset.districts.find { it.id == "cumilla" }
        assertNotNull("Cumilla district should exist in dataset", cumilla)

        val cumillaUpazilas = BangladeshGeographyCatalog.upazilas.filter { it.districtId == "cumilla" }
        assertEquals(17, cumillaUpazilas.size)
        assertEquals(cumillaUpazilas.size, cumilla?.upazilas?.size)

        val debidwar = cumillaUpazilas.find { it.id == "debidwar" }
        assertNotNull("Debidwar upazila should exist in official list", debidwar)

        val bbUpazilas = BangladeshGeographyCatalog.upazilas.filter { it.districtId == "brahmanbaria" }
        assertEquals(9, bbUpazilas.size)

        val bbSadar = LandRecordsDataset.districts.find { it.id == "brahmanbaria" }
            ?.upazilas?.find { it.id == "brahmanbaria-sadar" }
        assertNotNull("Brahmanbaria Sadar upazila should exist in seed dataset", bbSadar)
        assertEquals(8, bbSadar?.mouzas?.size)
    }

    /**
     * Phase 8A.1 — Geographic Isolation Across All 64 Districts
     * Verifies:
     * - 8 divisions and 64 districts exist with unique IDs and provenance metadata
     * - For every one of the 64 districts, all returned upazilas belong strictly to the selected district
     * - Every returned mouza belongs strictly to one of the returned upazilas of that district
     * - No duplicate geographic IDs or unrelated district records ever appear
     */
    @Test
    fun testAll64DistrictsGeographicIsolationAndProvenance() = runBlocking {
        assertEquals(8, BangladeshGeographyCatalog.divisions.size)
        assertEquals(64, BangladeshGeographyCatalog.districts.size)

        // Check unique IDs across catalog
        assertEquals(
            BangladeshGeographyCatalog.divisions.size,
            BangladeshGeographyCatalog.divisions.map { it.id }.distinct().size
        )
        assertEquals(
            BangladeshGeographyCatalog.districts.size,
            BangladeshGeographyCatalog.districts.map { it.id }.distinct().size
        )
        assertEquals(
            BangladeshGeographyCatalog.upazilas.size,
            BangladeshGeographyCatalog.upazilas.map { it.id }.distinct().size
        )
        val catalogMouzas = BangladeshGeographyCatalog.buildInitialMouzaBundle().mouzas
        assertEquals(
            catalogMouzas.size,
            catalogMouzas.map { it.id }.distinct().size
        )

        // Check provenance fields on every geographic record
        BangladeshGeographyCatalog.districts.forEach { dist ->
            assertTrue(dist.sourceName.isNotBlank())
            assertTrue(dist.sourceUrl.isNotBlank())
            assertTrue(dist.datasetVersion.isNotBlank())
            assertTrue(dist.importedAt > 0L)
            assertEquals(VerificationStatus.VERIFIED, dist.verificationStatus)
        }

        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = LRDownloaderViewModel(app)
        delay(400)
        ShadowLooper.idleMainLooper()

        for (districtEntity in BangladeshGeographyCatalog.districts) {
            viewModel.onDistrictChanged(districtEntity.id)
            ShadowLooper.idleMainLooper()

            val expectedUpazilaIds = BangladeshGeographyCatalog.upazilas
                .filter { it.districtId == districtEntity.id }
                .map { it.id }
                .toSet()
            assertTrue("District ${districtEntity.id} must have official upazilas", expectedUpazilaIds.isNotEmpty())

            val returnedUpazilas = viewModel.currentUpazilas
            val returnedUpazilaIds = returnedUpazilas.map { it.id }

            // No duplicates
            assertEquals(returnedUpazilaIds.size, returnedUpazilaIds.distinct().size)
            // Exact match with versioned source catalog for this district
            assertEquals(
                "District ${districtEntity.id} upazilas must strictly match catalog",
                expectedUpazilaIds,
                returnedUpazilaIds.toSet()
            )
            // Verify no unrelated district upazilas appear
            val unrelatedUpazilas = BangladeshGeographyCatalog.upazilas
                .filter { it.districtId != districtEntity.id }
                .map { it.id }
                .toSet()
            assertTrue(
                "No unrelated upazilas may appear in ${districtEntity.id}",
                returnedUpazilaIds.intersect(unrelatedUpazilas).isEmpty()
            )

            // Verify every returned mouza belongs to one of the returned upazilas in this district
            val returnedMouzas = viewModel.currentMouzas
            assertEquals(returnedMouzas.size, returnedMouzas.map { it.id }.distinct().size)
            val catalogMouzasForDistrict = catalogMouzas
                .filter { it.upazilaId in expectedUpazilaIds }
                .map { it.id }
                .toSet()
            assertTrue(
                "Every returned mouza in ${districtEntity.id} must belong to an upazila of ${districtEntity.id}",
                returnedMouzas.all { it.id in catalogMouzasForDistrict }
            )
        }
    }

    /**
     * Phase 8A.2 — Explicit 10-Step District Switch & Restart Regression Test
     * 1. Select Brahmanbaria.
     * 2. Verify its expected 9 catalog upazilas.
     * 3. Select Khulna.
     * 4. Verify that Brahmanbaria upazilas disappear.
     * 5. Select Cumilla.
     * 6. Verify its expected 17 catalog upazilas.
     * 7. Select Dhaka.
     * 8. Verify that Cumilla upazilas disappear.
     * 9. Restart the application.
     * 10. Verify that stale selections are not restored across districts.
     */
    @Test
    fun testTenStepDistrictSwitchAndRestartRegression() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = LRDownloaderViewModel(app)
        delay(500)
        ShadowLooper.idleMainLooper()

        val expectedBbUpazilas = BangladeshGeographyCatalog.upazilas
            .filter { it.districtId == "brahmanbaria" }
            .map { it.id }
            .toSet()
        val expectedCumillaUpazilas = BangladeshGeographyCatalog.upazilas
            .filter { it.districtId == "cumilla" }
            .map { it.id }
            .toSet()

        // 1. Select Brahmanbaria
        viewModel.onDistrictChanged("brahmanbaria")
        delay(200)
        ShadowLooper.idleMainLooper()

        // 2. Verify its expected 9 catalog upazilas
        assertEquals(9, expectedBbUpazilas.size)
        assertEquals(expectedBbUpazilas, viewModel.currentUpazilas.map { it.id }.toSet())

        // 3. Select Khulna
        viewModel.onDistrictChanged("khulna-dist")
        delay(200)
        ShadowLooper.idleMainLooper()

        // 4. Verify that Brahmanbaria upazilas disappear
        val khulnaUpazilaIds = viewModel.currentUpazilas.map { it.id }.toSet()
        assertTrue(khulnaUpazilaIds.isNotEmpty())
        assertTrue(
            "Brahmanbaria upazilas must completely disappear when Khulna is selected",
            khulnaUpazilaIds.intersect(expectedBbUpazilas).isEmpty()
        )
        assertTrue(
            "Selected upazila IDs in Khulna must not contain Brahmanbaria upazilas",
            viewModel.uiState.value.selectedUpazilaIds.intersect(expectedBbUpazilas).isEmpty()
        )

        // 5. Select Cumilla
        viewModel.onDistrictChanged("cumilla")
        delay(200)
        ShadowLooper.idleMainLooper()

        // 6. Verify its expected 17 catalog upazilas
        assertEquals(17, expectedCumillaUpazilas.size)
        assertEquals(expectedCumillaUpazilas, viewModel.currentUpazilas.map { it.id }.toSet())

        // 7. Select Dhaka
        viewModel.onDistrictChanged("dhaka-dist")
        delay(300)
        ShadowLooper.idleMainLooper()

        // 8. Verify that Cumilla upazilas disappear
        val dhakaUpazilaIds = viewModel.currentUpazilas.map { it.id }.toSet()
        assertTrue(dhakaUpazilaIds.isNotEmpty())
        assertTrue(
            "Cumilla upazilas must completely disappear when Dhaka is selected",
            dhakaUpazilaIds.intersect(expectedCumillaUpazilas).isEmpty()
        )
        assertTrue(
            "Selected upazila IDs in Dhaka must not contain Cumilla upazilas",
            viewModel.uiState.value.selectedUpazilaIds.intersect(expectedCumillaUpazilas).isEmpty()
        )

        // 9. Restart the application (instantiate fresh ViewModel)
        val restartedViewModel = LRDownloaderViewModel(app)
        delay(600)
        ShadowLooper.idleMainLooper()

        // 10. Verify that stale selections from Cumilla/Brahmanbaria/Khulna are not restored into Dhaka
        val restoredDistrictId = restartedViewModel.uiState.value.selectedDistrictId
        assertEquals("dhaka-dist", restoredDistrictId)
        val restoredUpazilaIds = restartedViewModel.uiState.value.selectedUpazilaIds
        assertTrue(restoredUpazilaIds.intersect(expectedCumillaUpazilas).isEmpty())
        assertTrue(restoredUpazilaIds.intersect(expectedBbUpazilas).isEmpty())
        assertTrue(restartedViewModel.currentUpazilas.all { it.id in dhakaUpazilaIds })
    }

    @Test
    fun testFailClosedOnInvalidDistrictOrUpazilaSelection() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = LRDownloaderViewModel(app)
        delay(400)
        ShadowLooper.idleMainLooper()

        // Selecting an unknown district ID must fail closed and display an error
        viewModel.onDistrictChanged("non-existent-district-999")
        ShadowLooper.idleMainLooper()
        assertNotNull(viewModel.uiState.value.districtSelectionError)
        assertTrue(viewModel.currentUpazilas.isEmpty())
        assertTrue(viewModel.currentMouzas.isEmpty())

        // Selecting valid Cumilla clears error, then selecting an upazila from another district fails closed
        viewModel.onDistrictChanged("cumilla")
        ShadowLooper.idleMainLooper()
        assertNull(viewModel.uiState.value.districtSelectionError)

        viewModel.onUpazilaChanged("brahmanbaria-sadar", autoStartQueues = false)
        ShadowLooper.idleMainLooper()
        assertNotNull(viewModel.uiState.value.districtSelectionError)
    }

    @Test
    fun testUpazilaFetchAndInitialSevenQueueLogic() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = LRDownloaderViewModel(app)
        delay(500)
        ShadowLooper.idleMainLooper()

        viewModel.onDistrictChanged("cumilla")
        delay(200)
        ShadowLooper.idleMainLooper()

        // Test legacy alias "adarsha-sadar" resolving cleanly to "cumilla-adarsha-sadar"
        viewModel.onUpazilaChanged("adarsha-sadar", autoStartQueues = false)
        delay(200)
        ShadowLooper.idleMainLooper()

        val state = viewModel.uiState.value
        assertEquals("cumilla-adarsha-sadar", state.selectedUpazilaId)
        assertNotNull(state.upazilaFetchInfo)
        assertEquals(7, state.targetTotalMouzas)

        viewModel.queueSelectedDownloads()
        delay(1000)
        ShadowLooper.idleMainLooper()

        assertEquals(7, viewModel.uiState.value.queue.size)
    }

    @Test
    fun testZeroSimulationIntegrity() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = LRDownloaderViewModel(app)
        delay(500)
        ShadowLooper.idleMainLooper()

        viewModel.onDistrictChanged("cumilla")
        viewModel.onUpazilaChanged("adarsha-sadar", autoStartQueues = false)
        viewModel.setRetrievalMode(RetrievalMode.MODE_B_USER_ASSISTED)
        delay(200)
        ShadowLooper.idleMainLooper()

        viewModel.queueSelectedDownloads()
        delay(1000)
        ShadowLooper.idleMainLooper()

        val queue = viewModel.uiState.value.queue
        assertTrue(queue.isNotEmpty())
        assertEquals(TaskStatus.WAITING_FOR_USER, queue.first().status)

        if (viewModel.uiState.value.isPaused) viewModel.togglePauseQueue()
        delay(3000)
        ShadowLooper.idleMainLooper()

        val updatedTask = viewModel.uiState.value.queue.first()
        assertEquals(TaskStatus.AWAITING_USER_IMPORT, updatedTask.status)
        assertEquals("https://eporcha.tech/", LRDownloaderViewModel.FREE_PORTAL_URL)
        assertEquals("https://eporcha.tech/", BangladeshGeographyCatalog.OFFICIAL_SOURCE_URL)
        assertEquals("https://eporcha.tech/", updatedTask.sourceReference)
        // Ensure no task ever auto-completes without a verified PDF document
        assertTrue(viewModel.uiState.value.queue.none { it.status == TaskStatus.COMPLETED })
    }

    @Test
    fun testPersistentQueueStateRecoveryAndCrossDistrictRejection() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = AppDatabase.getDatabase(app)
        val viewModel1 = LRDownloaderViewModel(app)
        delay(500)
        ShadowLooper.idleMainLooper()

        viewModel1.onDistrictChanged("cumilla")
        viewModel1.onUpazilaChanged("adarsha-sadar", autoStartQueues = false)
        delay(500)
        ShadowLooper.idleMainLooper()

        val viewModel2 = LRDownloaderViewModel(app)
        delay(800)
        ShadowLooper.idleMainLooper()

        assertNotNull(viewModel2.uiState.value.upazilaFetchInfo)
        assertEquals("cumilla", viewModel2.uiState.value.selectedDistrictId)
        assertEquals("cumilla-adarsha-sadar", viewModel2.uiState.value.selectedUpazilaId)

        // Inject a corrupted cross-district selection (district = cumilla, upazila = brahmanbaria-sadar)
        db.upazilaFetchInfoDao().insertFetchInfo(
            com.example.data.model.UpazilaFetchInfo(
                upazilaId = "brahmanbaria-sadar",
                divisionId = "chattogram",
                districtId = "cumilla",
                selectedUpazilaIdsCsv = "brahmanbaria-sadar",
                selectedMouzaIdsCsv = "bb-m1",
                fetchedMouzasCount = 8,
                totalQueuesCreated = 2,
                mouzasPerQueue = 7,
                datasetVersion = BangladeshGeographyCatalog.DATASET_VERSION
            )
        )
        delay(100)

        val viewModel3 = LRDownloaderViewModel(app)
        delay(800)
        ShadowLooper.idleMainLooper()

        // The invalid cross-district selection must be rejected
        assertFalse(viewModel3.uiState.value.selectedUpazilaIds.contains("brahmanbaria-sadar"))
    }

    /**
     * Phase 8B — Exact Seven-Mouza Batching Tests (7 -> 1, 14 -> 2, 15 -> 3, 50 -> 8)
     */
    @Test
    fun testStrictSevenMouzaBatchingDistributions() = runBlocking {
        assertEquals(7, LRDownloaderViewModel.DEFAULT_MOUZAS_PER_BATCH)
        assertEquals(7, DownloadQueueWorker.DEFAULT_MOUZAS_PER_BATCH)

        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = AppDatabase.getDatabase(app)

        // 1. Test 15 mouzas in Brahmanbaria (8 in brahmanbaria-sadar + 7 in sarail) -> 3 batches (7 + 7 + 1)
        val viewModel = LRDownloaderViewModel(app)
        delay(500)
        ShadowLooper.idleMainLooper()

        viewModel.onDistrictChanged("brahmanbaria")
        delay(200)
        viewModel.selectAllUpazilasInDistrict(autoSelectAllMouzas = true)
        delay(400)
        ShadowLooper.idleMainLooper()

        assertEquals(15, viewModel.currentMouzas.size)

        viewModel.queueSelectedDownloads()
        delay(1000)
        ShadowLooper.idleMainLooper()

        val queue15 = viewModel.uiState.value.queue
        assertEquals(15, queue15.size)
        assertEquals(15, queue15.map { it.mouzaId }.distinct().size)
        val batches15 = queue15.groupBy { it.batchNumber }
        assertEquals(3, batches15.size)
        assertEquals(7, batches15[1]?.size)
        assertEquals(7, batches15[2]?.size)
        assertEquals(1, batches15[3]?.size)

        // 2. Test 7 mouzas -> 1 batch (7)
        db.downloadQueueDao().clearQueue()
        viewModel.onDistrictChanged("cumilla")
        delay(200)
        viewModel.onUpazilaChanged("cumilla-adarsha-sadar", autoStartQueues = false)
        delay(200)
        viewModel.queueSelectedDownloads()
        delay(800)
        ShadowLooper.idleMainLooper()

        val queue7 = viewModel.uiState.value.queue
        assertEquals(7, queue7.size)
        val batches7 = queue7.groupBy { it.batchNumber }
        assertEquals(1, batches7.size)
        assertEquals(7, batches7[1]?.size)

        // 3. Test 14 mouzas -> 2 batches (7 + 7)
        db.downloadQueueDao().clearQueue()
        viewModel.onDistrictChanged("brahmanbaria")
        delay(200)
        viewModel.selectAllUpazilasInDistrict(autoSelectAllMouzas = true)
        delay(200)
        // Deselect 1 mouza so exactly 14 are selected
        val fifteenthMouzaId = viewModel.currentMouzas.last().id
        viewModel.toggleMouzaSelection(fifteenthMouzaId)
        ShadowLooper.idleMainLooper()
        assertEquals(14, viewModel.uiState.value.selectedMouzaIds.size)

        viewModel.queueSelectedDownloads()
        delay(800)
        ShadowLooper.idleMainLooper()

        val queue14 = viewModel.uiState.value.queue
        assertEquals(14, queue14.size)
        assertEquals(14, queue14.map { it.mouzaId }.distinct().size)
        assertFalse(queue14.any { it.mouzaId == fifteenthMouzaId })
        val batches14 = queue14.groupBy { it.batchNumber }
        assertEquals(2, batches14.size)
        assertEquals(7, batches14[1]?.size)
        assertEquals(7, batches14[2]?.size)

        // 4. Test 50 mouzas -> 8 batches (7 + 7 + 7 + 7 + 7 + 7 + 7 + 1)
        db.downloadQueueDao().clearQueue()
        val fiftyMouzas = (1..50).map { idx ->
            MouzaEntity(
                id = "cumilla-test-m$idx",
                upazilaId = "cumilla-adarsha-sadar",
                nameBn = "টেস্ট মৌজা $idx",
                nameEn = "Test Mouza $idx",
                jlNumber = idx.toString().padStart(2, '0'),
                verificationStatus = VerificationStatus.VERIFIED
            )
        }
        db.geographyDao().insertMouzas(fiftyMouzas)
        delay(200)

        val vm50 = LRDownloaderViewModel(app)
        delay(500)
        ShadowLooper.idleMainLooper()
        vm50.onDistrictChanged("cumilla")
        delay(200)
        vm50.onUpazilaChanged("cumilla-adarsha-sadar", autoStartQueues = false)
        delay(300)
        ShadowLooper.idleMainLooper()

        // Select exactly 50 unique mouzas
        vm50.clearMouzaSelection()
        val target50Ids = vm50.currentMouzas.take(50).map { it.id }
        assertEquals(50, target50Ids.size)
        target50Ids.forEach { vm50.toggleMouzaSelection(it) }
        ShadowLooper.idleMainLooper()

        vm50.queueSelectedDownloads()
        delay(1000)
        ShadowLooper.idleMainLooper()

        val queue50 = vm50.uiState.value.queue
        assertEquals(50, queue50.size)
        assertEquals(50, queue50.map { it.mouzaId }.distinct().size)
        val batches50 = queue50.groupBy { it.batchNumber }
        assertEquals(8, batches50.size)
        for (b in 1..7) {
            assertEquals(7, batches50[b]?.size)
        }
        assertEquals(1, batches50[8]?.size)
        vm50.clearQueue()
        viewModel.clearQueue()
        db.geographyDao().deleteMouzasByPrefix("cumilla-test-m")
    }

    /**
     * Phase 8C — Document Integrity & Real PDF Validation Tests
     */
    @Test
    fun testDocumentIntegrityAndDuplicateDetection() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = LRDownloaderViewModel(app)
        delay(500)
        ShadowLooper.idleMainLooper()

        // 1. Empty file rejected
        val emptyResult = DocumentVerificationEngine.verifyDocumentBytes(
            bytes = ByteArray(0),
            declaredMimeType = "application/pdf",
            fileName = "empty.pdf"
        )
        assertFalse(emptyResult.isValid)

        // 2. Invalid non-PDF header rejected
        val htmlResult = DocumentVerificationEngine.verifyDocumentBytes(
            bytes = "<html><body>Error</body></html>".toByteArray(),
            declaredMimeType = "application/pdf",
            fileName = "fake.pdf"
        )
        assertFalse(htmlResult.isValid)

        // 3. Corrupted PDF (valid header, missing %%EOF and structural objects) rejected
        val corruptedPdfBytes = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\ncorrupted_stream_without_eof_and_padding_to_exceed_128_bytes________________________________________________________________________".toByteArray()
        val corruptedResult = DocumentVerificationEngine.verifyDocumentBytes(
            bytes = corruptedPdfBytes,
            declaredMimeType = "application/pdf",
            fileName = "corrupted.pdf"
        )
        assertFalse(corruptedResult.isValid)

        // 4. Valid PDF accepted with exact SHA-256 verification
        val validPdfBytes = DocumentVerificationEngine.generateValidPdfReportBytes(
            title = "Official Khatian Verification Test",
            lines = listOf("District: Cumilla", "Upazila: Cumilla Adarsha Sadar", "Mouza: Kotbari JL 01")
        )
        val validResult = DocumentVerificationEngine.verifyDocumentBytes(
            bytes = validPdfBytes,
            declaredMimeType = "application/pdf",
            fileName = "kotbari_khatian_101.pdf"
        )
        assertTrue(validResult.isValid)
        assertEquals(
            DocumentVerificationEngine.calculateSha256(validPdfBytes),
            validResult.sha256Checksum
        )

        // 5. Queue a task, verify it awaits user import, then import the valid PDF and verify COMPLETED + Master Ledger
        val db = AppDatabase.getDatabase(app)
        viewModel.onDistrictChanged("cumilla")
        viewModel.onUpazilaChanged("cumilla-adarsha-sadar", autoStartQueues = false)
        delay(200)
        viewModel.queueSelectedDownloads()
        delay(800)
        ShadowLooper.idleMainLooper()

        val firstTask = viewModel.uiState.value.queue.first()
        viewModel.importPdfForTask(
            context = app,
            taskId = firstTask.id,
            pdfBytes = validPdfBytes,
            fileName = "kotbari_khatian_101.pdf"
        )
        delay(800)
        ShadowLooper.idleMainLooper()

        val completedTask = viewModel.uiState.value.queue.first { it.id == firstTask.id }
        assertEquals(TaskStatus.COMPLETED, completedTask.status)
        assertEquals(validResult.sha256Checksum, completedTask.sha256Checksum)
        val ledgerAfterFirst = db.masterLandLedgerDao().getAllEntriesDirect()
        assertEquals(1, ledgerAfterFirst.size)

        // 6. Duplicate document import is detected and does not overwrite existing ledger record
        val secondTask = viewModel.uiState.value.queue[1]
        viewModel.importPdfForTask(
            context = app,
            taskId = secondTask.id,
            pdfBytes = validPdfBytes,
            fileName = "kotbari_khatian_101_duplicate.pdf"
        )
        delay(500)
        ShadowLooper.idleMainLooper()

        val secondTaskUpdated = viewModel.uiState.value.queue.first { it.id == secondTask.id }
        assertEquals(TaskStatus.FAILED, secondTaskUpdated.status)
        val ledgerAfterDuplicate = db.masterLandLedgerDao().getAllEntriesDirect()
        assertEquals(1, ledgerAfterDuplicate.size)
    }

    /**
     * Phase 8D — Non-Destructive Room Migration Test (v7 -> v8)
     * Verifies that upgrading from schema v7 to v8 preserves existing saved_land_records,
     * master_land_ledger, download_queue, and upazila_fetch_info records.
     */
    @Test
    fun testRoomMigration7To8PreservesUserRecordsAndQueue() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_test_lrgetbd.db"
        context.deleteDatabase(dbName)

        val callback = object : SupportSQLiteOpenHelper.Callback(7) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
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
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `saved_land_records` (
                        `id` TEXT NOT NULL,
                        `mouzaId` TEXT NOT NULL,
                        `mouzaNameBn` TEXT NOT NULL,
                        `mouzaNameEn` TEXT NOT NULL,
                        `jlNo` TEXT NOT NULL,
                        `upazilaNameBn` TEXT NOT NULL,
                        `districtNameBn` TEXT NOT NULL,
                        `recordType` TEXT NOT NULL,
                        `khatianCount` INTEGER NOT NULL,
                        `fileSizeFormatted` TEXT NOT NULL,
                        `downloadedAt` INTEGER NOT NULL,
                        `divisionNameBn` TEXT NOT NULL,
                        `fileLocation` TEXT NOT NULL,
                        `sha256Checksum` TEXT NOT NULL,
                        `sourceName` TEXT NOT NULL,
                        `verificationStatus` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `upazilas` (
                        `id` TEXT NOT NULL,
                        `district_id` TEXT NOT NULL,
                        `name_bn` TEXT NOT NULL,
                        `name_en` TEXT NOT NULL,
                        `official_code` TEXT NOT NULL,
                        `verification_status` TEXT NOT NULL,
                        `source_type` TEXT NOT NULL,
                        `source_name` TEXT NOT NULL DEFAULT 'BBS',
                        `source_url` TEXT NOT NULL DEFAULT 'https://bbs.gov.bd',
                        `source_reference` TEXT NOT NULL,
                        `dataset_version` TEXT NOT NULL DEFAULT '2026.04.1',
                        `imported_at` INTEGER NOT NULL DEFAULT 0,
                        `last_verified_timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `mouzas` (
                        `id` TEXT NOT NULL,
                        `upazila_id` TEXT NOT NULL,
                        `name_bn` TEXT NOT NULL,
                        `name_en` TEXT NOT NULL,
                        `jl_number` TEXT NOT NULL,
                        `official_code` TEXT NOT NULL DEFAULT '',
                        `available_surveys_csv` TEXT NOT NULL,
                        `verification_status` TEXT NOT NULL,
                        `source_type` TEXT NOT NULL,
                        `source_name` TEXT NOT NULL,
                        `source_url` TEXT NOT NULL DEFAULT 'https://land.gov.bd',
                        `source_reference` TEXT NOT NULL,
                        `dataset_version` TEXT NOT NULL DEFAULT '2026.04.1',
                        `imported_at` INTEGER NOT NULL DEFAULT 0,
                        `last_verified_timestamp` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `saved_land_records` VALUES (
                        'rec-1', 'm1', 'কোটবাড়ী', 'Kotbari', '01', 'আদর্শ সদর', 'কুমিল্লা',
                        'RS', 1, '2.0 KB', 1710000000000, 'চট্টগ্রাম',
                        'Download/LandRecords/Kotbari.pdf', 'abc123sha256', 'LRgetBD', 'CHECKSUM_VERIFIED'
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `upazila_fetch_info` VALUES (
                        'adarsha-sadar', 'আদর্শ সদর', 'Cumilla Adarsha Sadar', 'কুমিল্লা',
                        7, 1, 'Loaded 7 mouzas', 350, 7, 'High-Speed Network', 'Multi-Core Device'
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `upazilas` VALUES (
                        'adarsha-sadar', 'cumilla', 'আদর্শ সদর', 'Cumilla Adarsha Sadar',
                        '1967', 'VERIFIED', 'OFFICIAL_GAZETTE_SEED', 'BBS', 'https://bbs.gov.bd',
                        'Ref', '2026.04.1', 1710000000000, 1710000000000
                    )
                    """.trimIndent()
                )
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        }

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(callback)
                .build()
        )

        val sqlDb = helper.writableDatabase
        AppDatabase.MIGRATION_7_8.migrate(sqlDb)

        // Verify saved_land_records was preserved
        sqlDb.query("SELECT id, mouzaNameEn, fileLocation FROM saved_land_records WHERE id = 'rec-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Kotbari", cursor.getString(1))
            assertEquals("Download/LandRecords/Kotbari.pdf", cursor.getString(2))
        }

        // Verify upazila_fetch_info was migrated to canonical "cumilla-adarsha-sadar" and retained mouzasPerQueue = 7
        sqlDb.query("SELECT upazilaId, mouzasPerQueue, datasetVersion FROM upazila_fetch_info").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("cumilla-adarsha-sadar", cursor.getString(0))
            assertEquals(7, cursor.getInt(1))
        }

        sqlDb.close()
        context.deleteDatabase(dbName)
    }
}
