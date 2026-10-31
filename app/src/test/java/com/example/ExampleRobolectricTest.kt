package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.LandRecordsDataset
import com.example.data.model.RecordType
import com.example.ui.AppScreen
import com.example.ui.LRDownloaderViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.test.runTest
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun testAppNameString() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LR get BD", appName)
    }

    @Test
    fun testDatasetIntegrityWithRealisticMouzaCounts() {
        val cumilla = LandRecordsDataset.districts.find { it.id == "cumilla" }
        assertNotNull(cumilla)
        assertEquals(17, cumilla?.upazilas?.size)

        val debidwar = cumilla?.upazilas?.find { it.id == "debidwar" }
        assertNotNull(debidwar)
        assertEquals(136, debidwar?.mouzas?.size)

        val brahmanbaria = LandRecordsDataset.districts.find { it.id == "brahmanbaria" }
        assertNotNull(brahmanbaria)
        assertEquals(9, brahmanbaria?.upazilas?.size)

        val bbSadar = brahmanbaria?.upazilas?.find { it.id == "brahmanbaria-sadar" }
        assertNotNull(bbSadar)
        assertEquals(126, bbSadar?.mouzas?.size)
    }

    @Test
    fun testUpazilaFetchAndInitialSevenQueueLogic() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = LRDownloaderViewModel(app)

        // 1. Initial State
        viewModel.onDistrictChanged("cumilla")
        ShadowLooper.idleMainLooper()
        assertEquals("cumilla", viewModel.uiState.value.selectedDistrictId)
        assertNull(viewModel.uiState.value.selectedUpazilaId)
        assertNull(viewModel.uiState.value.upazilaFetchInfo)

        // 2. Select Upazila (without auto-start so we can inspect selection & multi-queue plan)
        viewModel.onUpazilaChanged("debidwar", autoStartQueues = false)
        ShadowLooper.idleMainLooper()

        val state = viewModel.uiState.value
        assertEquals("debidwar", state.selectedUpazilaId)
        assertNotNull(state.upazilaFetchInfo)
        assertEquals(136, state.targetTotalMouzas)
        assertEquals(136, state.upazilaFetchInfo?.totalMouzaCount)

        // Verify selecting an Upazila automatically selects ALL 136 Mouzas and ALL Land Record Types at once
        assertEquals(136, state.selectedMouzaIds.size)
        assertEquals(RecordType.values().size, state.selectedRecordTypes.size)
        assertTrue(state.selectedLandRecordsCount > 0)
        assertNotNull(state.deviceNetworkProfile)
        assertTrue(state.plannedQueues.size > 1)

        // 3. Test Enqueue creates multiple sequential queues (Batch #1 .. Batch #N) for all 136 Mouzas
        viewModel.queueSelectedDownloads()
        ShadowLooper.idleMainLooper()

        assertEquals(136, viewModel.uiState.value.queue.size)
        val distinctQueueNumbers = viewModel.uiState.value.queue.map { it.batchNumber }.distinct().sorted()
        assertTrue(distinctQueueNumbers.size > 1)
        assertEquals(1, distinctQueueNumbers.first())
    }

    @Test
    fun testLandRecordStorageManagerWritesToDownloads() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = com.example.data.local.LandRecordStorageManager.saveFileToLandRecords(
            context,
            "test_mouza.json",
            "{\"test\": true}",
            "application/json"
        )
        assertNotNull(result)
        assertTrue(result.primaryDisplayPath.contains("LandRecords"))
    }

    @Test
    fun testWorkManagerBackgroundServiceConfiguration() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = LRDownloaderViewModel(app)

        // Verify default WorkManager configuration
        assertTrue(viewModel.uiState.value.useWorkManagerBackgroundService)
        assertTrue(viewModel.uiState.value.antiBlockingDelayDescription.contains("Jitter"))

        // Verify toggle functionality
        viewModel.setUseWorkManagerBackgroundService(false)
        assertFalse(viewModel.uiState.value.useWorkManagerBackgroundService)

        viewModel.setUseWorkManagerBackgroundService(true)
        assertTrue(viewModel.uiState.value.useWorkManagerBackgroundService)
    }

    @Test
    fun testQueueStatusScreenNavigation() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = LRDownloaderViewModel(app)

        // Verify initial screen
        assertEquals(AppScreen.DOWNLOADER, viewModel.uiState.value.currentScreen)

        // Navigate to Queue Status screen
        viewModel.navigateToScreen(AppScreen.QUEUE_STATUS)
        assertEquals(AppScreen.QUEUE_STATUS, viewModel.uiState.value.currentScreen)

        // Navigate back to Downloader screen
        viewModel.navigateToScreen(AppScreen.DOWNLOADER)
        assertEquals(AppScreen.DOWNLOADER, viewModel.uiState.value.currentScreen)
    }

    @Test
    fun testParseReferenceIdsFlexibleInput() {
        // 1. Comma, space, and newline separated input
        val input1 = "101, 102, 103\n104  105; 106"
        val parsed1 = LRDownloaderViewModel.parseReferenceIds(input1)
        assertEquals(listOf("101", "102", "103", "104", "105", "106"), parsed1)

        // 2. Range expansion (e.g. 50-54)
        val input2 = "50-54, 88"
        val parsed2 = LRDownloaderViewModel.parseReferenceIds(input2)
        assertEquals(listOf("50", "51", "52", "53", "54", "88"), parsed2)

        // 3. Alphanumeric reference IDs with duplicates
        val input3 = "LR-2024-001, LR-2024-002, LR-2024-001"
        val parsed3 = LRDownloaderViewModel.parseReferenceIds(input3)
        assertEquals(listOf("LR-2024-001", "LR-2024-002"), parsed3)

        // 4. Empty and blank input
        val parsed4 = LRDownloaderViewModel.parseReferenceIds("   ")
        assertTrue(parsed4.isEmpty())
    }

    @Test
    fun testSequentialReferenceIdsWorkManagerQueueing() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = LRDownloaderViewModel(app)

        val district = LandRecordsDataset.districts.first()
        val upazila = district.upazilas.first()
        val mouza = upazila.mouzas.first()

        val referenceIds = listOf("REF-101", "REF-102", "REF-103", "REF-104")
        val recordTypes = setOf(RecordType.RS)

        viewModel.queueReferenceIdsDownload(
            district = district,
            upazila = upazila,
            mouza = mouza,
            referenceIds = referenceIds,
            recordTypes = recordTypes
        )
        ShadowLooper.idleMainLooper()

        val queue = viewModel.uiState.value.queue
        assertEquals(4, queue.size)

        // Verify each task has its unique reference ID and sequential PENDING state
        for (i in referenceIds.indices) {
            val task = queue[i]
            assertEquals(referenceIds[i], task.referenceId)
            assertTrue(task.mouzaNameBn.contains(referenceIds[i]))
            assertEquals(com.example.data.model.TaskStatus.PENDING, task.status)
        }
    }

    @Test
    fun testBangladesh8DivisionsAnd64DistrictsHierarchy() {
        val divisions = com.example.data.geography.BangladeshGeographyCatalog.divisions
        val districts = com.example.data.geography.BangladeshGeographyCatalog.districts
        val upazilas = com.example.data.geography.BangladeshGeographyCatalog.upazilas

        // Verify all 8 administrative divisions of Bangladesh exist
        assertEquals(8, divisions.size)
        val expectedDivisions = setOf(
            "dhaka", "chattogram", "rajshahi", "khulna",
            "barishal", "sylhet", "rangpur", "mymensingh"
        )
        assertEquals(expectedDivisions, divisions.map { it.id }.toSet())

        // Verify all 64 official districts of Bangladesh exist and map to valid divisions
        assertEquals(64, districts.size)
        districts.forEach { dist ->
            assertTrue(dist.divisionId in expectedDivisions)
            assertTrue(dist.officialCode.isNotBlank())
        }

        // Verify every district has at least one verified Upazila
        districts.forEach { dist ->
            val distUpazilas = upazilas.filter { it.districtId == dist.id }
            assertTrue("District ${dist.nameEn} must have upazilas", distUpazilas.isNotEmpty())
        }
    }

    @Test
    fun testDuplicateMouzaDetectionAndJlNumberValidation() {
        assertTrue(com.example.data.geography.BangladeshGeographyCatalog.isValidJlNumber("১২"))
        assertTrue(com.example.data.geography.BangladeshGeographyCatalog.isValidJlNumber("136"))
        assertFalse(com.example.data.geography.BangladeshGeographyCatalog.isValidJlNumber(""))
        assertFalse(com.example.data.geography.BangladeshGeographyCatalog.isValidJlNumber("0"))
        assertFalse(com.example.data.geography.BangladeshGeographyCatalog.isValidJlNumber("ABC"))

        val m1 = com.example.data.geography.MouzaEntity(
            id = "deb-1",
            upazilaId = "debidwar",
            nameEn = "Debidwar",
            nameBn = "দেবিদ্বার",
            jlNumber = "১৪",
            officialMouzaCode = "DEB-14",
            sourceReference = "BBS",
            verificationStatus = com.example.data.geography.GeoVerificationStatus.VERIFIED,
            lastVerifiedAt = 1000L
        )
        val m2Duplicate = m1.copy(id = "deb-dup", jlNumber = "14")
        val duplicates = com.example.data.geography.BangladeshGeographyCatalog.detectDuplicateMouzas(listOf(m1, m2Duplicate))
        assertEquals(1, duplicates.size)
        assertEquals("deb-dup", duplicates.first().id)
    }

    @Test
    fun testCriticalAcceptanceSelectAllUpazilasAndSevenMouzaBatchQueue() = runTest {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = LRDownloaderViewModel(app)

        // 1. Select the district (Brahmanbaria: 9 Upazilas, 946 Verified Mouzas)
        viewModel.onDistrictChanged("brahmanbaria")
        ShadowLooper.idleMainLooper()
        assertEquals("brahmanbaria", viewModel.uiState.value.selectedDistrictId)

        // 2. Select all Upazilas & 3-4. Automatically load and select all associated verified Mouzas
        viewModel.selectAllUpazilasInDistrict(autoSelectAllMouzas = true)
        ShadowLooper.idleMainLooper()

        val stateAfterSelectAll = viewModel.uiState.value
        assertEquals(9, stateAfterSelectAll.selectedUpazilaIds.size)
        val totalMouzasInDistrict = viewModel.currentMouzas.size
        assertTrue(totalMouzasInDistrict > 500)
        assertEquals(totalMouzasInDistrict, stateAfterSelectAll.selectedMouzaIds.size)

        // 5. Pause before queueing so we can deterministically inspect the generated batches
        viewModel.togglePauseQueue()
        viewModel.queueSelectedDownloads()
        ShadowLooper.idleMainLooper()

        val queuedTasks = viewModel.uiState.value.queue
        // 6. Confirm that every eligible Mouza appears exactly once (duplicate-free)
        assertEquals(totalMouzasInDistrict, queuedTasks.size)
        assertEquals(totalMouzasInDistrict, queuedTasks.map { it.mouzaId }.distinct().size)

        // 7. Confirm that tasks are divided into batches of seven (7 Mouzas per batch)
        val batch1Tasks = queuedTasks.filter { it.batchNumber == 1 }
        val batch2Tasks = queuedTasks.filter { it.batchNumber == 2 }
        val batch3Tasks = queuedTasks.filter { it.batchNumber == 3 }
        assertEquals(7, batch1Tasks.size)
        assertEquals(7, batch2Tasks.size)
        assertEquals(7, batch3Tasks.size)

        val expectedTotalBatches = (totalMouzasInDistrict + 6) / 7
        assertEquals(expectedTotalBatches, queuedTasks.maxOf { it.batchNumber })
    }

    @Test
    fun testDocumentVerificationSha256AndPdfValidation() {
        // 1. Reject fake plain-text disguised as PDF
        val fakePdfBytes = "--- SIMULATED PDF LAND RECORD ---".toByteArray(Charsets.UTF_8)
        val fakeResult = com.example.util.DocumentVerificationEngine.verifyDocumentBytes(
            bytes = fakePdfBytes,
            declaredMimeType = "application/pdf",
            fileName = "fake.pdf"
        )
        assertFalse(fakeResult.isValid)
        assertFalse(fakeResult.isPdfStructureValid)

        // 2. Accept genuine binary PDF 1.4 with %PDF-1.4 header, objects, and %%EOF trailer
        val validPdfBytes = com.example.util.DocumentVerificationEngine.generateValidPdfReportBytes(
            title = "Test Archive Ledger",
            lines = listOf("Division: Chattogram | District: Cumilla | Upazila: Debidwar")
        )
        val validResult = com.example.util.DocumentVerificationEngine.verifyDocumentBytes(
            bytes = validPdfBytes,
            declaredMimeType = "application/pdf",
            fileName = "valid_ledger.pdf"
        )
        assertTrue(validResult.isValid)
        assertTrue(validResult.isPdfStructureValid)
        assertEquals(64, validResult.sha256Checksum.length)
    }

    @Test
    fun testGeographicDatabaseRuntimeJsonUpdate() {
        val sampleJson = """
            {
              "source_name": "BBS Gazetteer Update",
              "source_url": "https://bbs.gov.bd",
              "dataset_version": "2026.2",
              "mouzas": [
                {
                  "upazila_id": "debidwar",
                  "name_en": "Nabinagar North",
                  "name_bn": "নবীনগর উত্তর",
                  "jl_number": "140",
                  "verified": true
                },
                {
                  "upazila_id": "debidwar",
                  "name_en": "Nabinagar North",
                  "name_bn": "নবীনগর উত্তর",
                  "jl_number": "140",
                  "verified": true
                }
              ]
            }
        """.trimIndent()

        val parsed = com.example.data.geography.BangladeshGeographyCatalog.parseJsonGeographyUpdate(sampleJson)
        assertEquals(1, parsed.mouzas.size)
        assertEquals(1, parsed.errors.size) // Second identical entry caught as duplicate
    }
}
