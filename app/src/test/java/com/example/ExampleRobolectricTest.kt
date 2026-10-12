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
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun testAppNameString() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LR Mass Downloader", appName)
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

        // 2. Select Upazila: FIRST fetch how many mouzas in that upazila!
        viewModel.onUpazilaChanged("debidwar")
        ShadowLooper.idleMainLooper()

        val state = viewModel.uiState.value
        assertEquals("debidwar", state.selectedUpazilaId)
        assertNotNull(state.upazilaFetchInfo)
        assertEquals(136, state.targetTotalMouzas)
        assertEquals(136, state.upazilaFetchInfo?.totalMouzaCount)

        // Verify selector strictly selects the first 7 mouzas for Batch 1 (not all 136)
        assertEquals(7, state.selectedMouzaIds.size)

        // 3. Test Select All Documents
        viewModel.selectAllRecordTypes()
        assertEquals(4, viewModel.uiState.value.selectedRecordTypes.size)

        // 4. Test Enqueue
        viewModel.queueSelectedDownloads()
        ShadowLooper.idleMainLooper()

        assertEquals(7, viewModel.uiState.value.queue.size)
        viewModel.uiState.value.queue.forEach { task ->
            assertEquals(4, task.recordTypes.size)
        }

        // 5. Test Next 7 Selection without repeats
        viewModel.clearQueue()
        ShadowLooper.idleMainLooper()
        viewModel.selectNextSevenMouzas()
        assertEquals(7, viewModel.uiState.value.selectedMouzaIds.size)
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
}
