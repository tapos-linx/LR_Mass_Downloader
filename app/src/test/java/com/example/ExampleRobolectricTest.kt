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
import kotlinx.coroutines.delay
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @org.junit.Before
    fun setUp() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = com.example.data.local.AppDatabase.getDatabase(context)
        db.downloadQueueDao().clearQueue()
        db.savedRecordDao().clearAllRecords()
        db.masterLandLedgerDao().clearAll()
        db.upazilaFetchInfoDao().clearFetchInfo()
        // Wait for clear to commit
        delay(100)
    }

    @Test
    fun testAppNameString() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LR get BD", appName)
    }

    @Test
    fun testDatasetIntegrityWithRealisticMouzaCounts() {
        println("DEBUG: Starting testDatasetIntegrityWithRealisticMouzaCounts")
        val cumilla = LandRecordsDataset.districts.find { it.id == "cumilla" }
        assertNotNull("Cumilla district should exist in dataset", cumilla)
        
        val cumillaUpazilas = com.example.data.geography.BangladeshGeographyCatalog.upazilas.filter { it.districtId == "cumilla" }
        println("DEBUG: Cumilla upazilas count: ${cumillaUpazilas.size}")
        assertEquals(17, cumillaUpazilas.size)

        val debidwar = cumillaUpazilas.find { it.id == "debidwar" }
        assertNotNull("Debidwar upazila should exist in official list", debidwar)
        
        val bbUpazilas = com.example.data.geography.BangladeshGeographyCatalog.upazilas.filter { it.districtId == "brahmanbaria" }
        println("DEBUG: Brahmanbaria upazilas count: ${bbUpazilas.size}")
        assertEquals(9, bbUpazilas.size)

        val bbSadar = LandRecordsDataset.districts.find { it.id == "brahmanbaria" }
            ?.upazilas?.find { it.id == "brahmanbaria-sadar" }
        assertNotNull("Brahmanbaria Sadar upazila should exist in seed dataset", bbSadar)
        println("DEBUG: Brahmanbaria Sadar mouzas count: ${bbSadar?.mouzas?.size}")
        assertEquals(8, bbSadar?.mouzas?.size)
    }

    @Test
    fun testUpazilaFetchAndInitialSevenQueueLogic() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = LRDownloaderViewModel(app)
        delay(500)
        ShadowLooper.idleMainLooper()

        viewModel.onDistrictChanged("cumilla")
        delay(200)
        ShadowLooper.idleMainLooper()

        viewModel.onUpazilaChanged("adarsha-sadar", autoStartQueues = false)
        delay(200)
        ShadowLooper.idleMainLooper()

        val state = viewModel.uiState.value
        assertNotNull(state.upazilaFetchInfo)
        assertEquals(7, state.targetTotalMouzas)

        viewModel.queueSelectedDownloads()
        delay(1000)
        ShadowLooper.idleMainLooper()

        assertEquals(7, viewModel.uiState.value.queue.size)
    }

    @Test
    fun testZeroSimulationIntegrity() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = LRDownloaderViewModel(app)
        delay(500)
        ShadowLooper.idleMainLooper()

        viewModel.onDistrictChanged("cumilla")
        viewModel.onUpazilaChanged("adarsha-sadar", autoStartQueues = false)
        viewModel.setRetrievalMode(com.example.data.model.RetrievalMode.MODE_B_USER_ASSISTED)
        delay(200)
        ShadowLooper.idleMainLooper()

        viewModel.queueSelectedDownloads()
        delay(1000)
        ShadowLooper.idleMainLooper()

        val queue = viewModel.uiState.value.queue
        assertTrue(queue.isNotEmpty())
        assertEquals(com.example.data.model.TaskStatus.WAITING_FOR_USER, queue.first().status)

        if (viewModel.uiState.value.isPaused) viewModel.togglePauseQueue() 
        delay(3000)
        ShadowLooper.idleMainLooper()

        val updatedTask = viewModel.uiState.value.queue.first()
        assertEquals(com.example.data.model.TaskStatus.AWAITING_USER_IMPORT, updatedTask.status)
    }

    @Test
    fun testPersistentQueueStateRecovery() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel1 = LRDownloaderViewModel(app)
        delay(500)
        ShadowLooper.idleMainLooper()
        
        viewModel1.onDistrictChanged("cumilla")
        viewModel1.onUpazilaChanged("adarsha-sadar", autoStartQueues = false)
        delay(500)
        ShadowLooper.idleMainLooper()
        
        val viewModel2 = LRDownloaderViewModel(app)
        delay(1000)
        ShadowLooper.idleMainLooper()
        
        assertNotNull(viewModel2.uiState.value.upazilaFetchInfo)
    }

    @Test
    fun testStrictBatchingLogic() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = LRDownloaderViewModel(app)
        delay(500)
        ShadowLooper.idleMainLooper()

        viewModel.onDistrictChanged("brahmanbaria")
        delay(200)
        viewModel.selectAllUpazilasInDistrict(autoSelectAllMouzas = true)
        delay(500)
        ShadowLooper.idleMainLooper()
        
        assertEquals(15, viewModel.currentMouzas.size)
        
        viewModel.queueSelectedDownloads()
        delay(1000)
        ShadowLooper.idleMainLooper()
        
        val queue = viewModel.uiState.value.queue
        assertEquals(15, queue.size)
        
        val batches = queue.groupBy { it.batchNumber }
        assertEquals(3, batches.size)
        assertEquals(7, batches[1]?.size)
        assertEquals(7, batches[2]?.size)
        assertEquals(1, batches[3]?.size)
    }
}
