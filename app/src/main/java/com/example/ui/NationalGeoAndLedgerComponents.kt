package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.geography.DivisionEntity
import com.example.data.geography.GeographyCoverageSummary
import com.example.data.geography.GeographyImportHistoryEntity
import com.example.data.ledger.MasterLedgerEntryEntity
import com.example.data.model.District
import com.example.data.model.RetrievalMode
import com.example.data.model.Upazila
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberDark
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.BlueContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LogoNavy
import com.example.ui.theme.LogoTeal
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.util.DocumentVerificationEngine

/**
 * National 8-Division, 64-District, Multi-Upazila, and Select-All-Mouzas Explorer Card (Sections 3, 4, 5, 10).
 * Displays real database values and provides 1-click "SELECT ALL MOUZAS & CREATE DOWNLOAD QUEUE".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NationalGeographicExplorerCard(
    divisions: List<DivisionEntity>,
    selectedDivisionId: String,
    districtsInDivision: List<District>,
    selectedDistrictId: String,
    upazilasInDistrict: List<Upazila>,
    selectedUpazilaIds: Set<String>,
    coverageSummary: GeographyCoverageSummary,
    selectedRetrievalMode: RetrievalMode,
    archivedDocsCount: Int,
    onSelectDivision: (String) -> Unit,
    onSelectDistrict: (String) -> Unit,
    onToggleUpazila: (String) -> Unit,
    onSelectAllUpazilas: () -> Unit,
    onDeselectAllUpazilas: () -> Unit,
    onSelectRetrievalMode: (RetrievalMode) -> Unit,
    onOpenPreQueueConfirmation: () -> Unit,
    onOpenPdfImportDialog: () -> Unit,
    onOpenGeoUpdateDialog: () -> Unit,
    onOpenMasterLedger: () -> Unit,
    onOpenUserAssistedBrowser: () -> Unit,
    onGenerateQueue: () -> Unit = {},
    onDownloadAll: () -> Unit = {}
) {
    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Slate200, RoundedCornerShape(24.dp))
            .testTag("national_geographic_explorer_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Bangladesh -> Division -> District -> Upazila -> Mouza
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(LogoNavy),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Bangladesh Geographic Database",
                            tint = LogoTeal,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "বাংলাদেশ জাতীয় ভৌগোলিক ডাটাবেস (৮ বিভাগ • ৬৪ জেলা)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Slate900
                            )
                        )
                        Text(
                            text = "Division ➔ District ➔ Upazila ➔ Mouza ➔ JL No ➔ 7-Mouza Batch Queue",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Slate600,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = EmeraldContainer
                ) {
                    Text(
                        text = "100% FREE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = EmeraldDark,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Real Database Metrics Strip (No Random or Simulated Stats)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GeoMetricChip("Divisions", "${coverageSummary.totalDivisions}", EmeraldDark, EmeraldContainer)
                GeoMetricChip("Districts", "${coverageSummary.totalDistricts}", BlueAccent, BlueContainer)
                GeoMetricChip("Upazilas", "${coverageSummary.totalUpazilas}", Slate800, Slate100)
                GeoMetricChip("Verified Mouzas", "${coverageSummary.totalVerifiedMouzas}", EmeraldDark, EmeraldContainer)
                GeoMetricChip("Selected Mouzas", "${coverageSummary.selectedMouzasCount}", AmberDark, AmberContainer)
                GeoMetricChip("Archived Docs", "$archivedDocsCount", BlueAccent, BlueContainer)
            }

            // Top-of-Homepage Primary Action Row: Queue Generator & Download All
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onGenerateQueue,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BlueAccent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                        .testTag("card_top_queue_generator_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "Queue Generator",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black)
                        )
                        Text(
                            text = "কিউ জেনারেটর (7/Batch)",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = Color.White.copy(alpha = 0.9f))
                        )
                    }
                }

                Button(
                    onClick = onDownloadAll,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                        .testTag("card_top_download_all_button")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "Download All",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black)
                        )
                        Text(
                            text = "সব ডাউনলোড করুন",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = Color.White.copy(alpha = 0.9f))
                        )
                    }
                }
            }

            // Step 1: Select Division (All 8 Divisions of Bangladesh)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "STEP 1: বিভাগ নির্বাচন করুন (Select Division — All 8 Divisions)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate700
                    )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    divisions.forEach { div ->
                        val isSelected = div.id == selectedDivisionId
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectDivision(div.id) },
                            label = {
                                Text(
                                    text = "${div.nameBn} (${div.nameEn})",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                    )
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LogoNavy,
                                selectedLabelColor = Color.White,
                                containerColor = Slate50,
                                labelColor = Slate800
                            ),
                            modifier = Modifier.testTag("division_chip_${div.id}")
                        )
                    }
                }
            }

            // Step 2: Select District within the chosen Division
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "STEP 2: জেলা নির্বাচন করুন (Verified Districts in Division: ${districtsInDivision.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate700
                    )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    districtsInDivision.forEach { dist ->
                        val isSelected = dist.id == selectedDistrictId
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectDistrict(dist.id) },
                            label = {
                                Text(
                                    text = "${dist.nameBn} (${dist.nameEn})",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                    )
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = Slate50,
                                labelColor = Slate800
                            ),
                            modifier = Modifier.testTag("national_district_chip_${dist.id}")
                        )
                    }
                }
            }

            // Step 3: Multi-Upazila Selector with Select All Upazilas & Deselect All Upazilas
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "STEP 3: উপজেলা নির্বাচন (${selectedUpazilaIds.size}/${upazilasInDistrict.size} Selected)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate700
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton(
                            onClick = onSelectAllUpazilas,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = EmeraldContainer,
                                contentColor = EmeraldDark
                            ),
                            modifier = Modifier
                                .heightIn(min = 36.dp)
                                .testTag("select_all_upazilas_button")
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Select All Upazilas",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold)
                            )
                        }

                        OutlinedButton(
                            onClick = onDeselectAllUpazilas,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .heightIn(min = 36.dp)
                                .testTag("deselect_all_upazilas_button")
                        ) {
                            Text(
                                text = "Deselect All",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    upazilasInDistrict.forEach { upa ->
                        val isSelected = upa.id in selectedUpazilaIds
                        FilterChip(
                            selected = isSelected,
                            onClick = { onToggleUpazila(upa.id) },
                            label = {
                                Text(
                                    text = "${upa.nameBn} (${upa.mouzas.size})",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                    )
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BlueContainer,
                                selectedLabelColor = BlueAccent,
                                containerColor = Slate50,
                                labelColor = Slate700
                            ),
                            modifier = Modifier.testTag("multi_upazila_chip_${upa.id}")
                        )
                    }
                }
            }

            // Retrieval Mode Selector (Mode A Authorized vs Mode B User-Assisted Browser)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "RETRIEVAL MODE (অনুমোদিত স্বয়ংক্রিয় বনাম ব্রাউজার সহায়ক মোড)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Slate700
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RetrievalMode.values().forEach { mode ->
                        val isSelected = selectedRetrievalMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) LogoNavy else Slate50)
                                .border(1.dp, if (isSelected) LogoTeal else Slate200, RoundedCornerShape(12.dp))
                                .clickable { onSelectRetrievalMode(mode) }
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                                .testTag("retrieval_mode_${mode.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.labelBn,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Slate800
                                )
                            )
                        }
                    }
                }
            }

            // Prominent Action Button: SELECT ALL MOUZAS & CREATE DOWNLOAD QUEUE
            Button(
                onClick = onOpenPreQueueConfirmation,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 14.dp, horizontal = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("select_all_mouzas_and_create_queue_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SELECT ALL MOUZAS & CREATE DOWNLOAD QUEUE (7/Batch)",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black)
                )
            }

            // Secondary Tools Toolbar: PDF Import, Master Ledger, Browser Assistant, Geo DB Update
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onOpenPdfImportDialog,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.testTag("open_pdf_import_button")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import & Verify PDF", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }

                FilledTonalButton(
                    onClick = onOpenMasterLedger,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.testTag("open_master_ledger_button")
                ) {
                    Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Master Land Ledger ($archivedDocsCount)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }

                FilledTonalButton(
                    onClick = onOpenUserAssistedBrowser,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.testTag("open_browser_assistant_button")
                ) {
                    Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Browser Assistant", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }

                FilledTonalButton(
                    onClick = onOpenGeoUpdateDialog,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.testTag("open_geo_update_button")
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Update Geo DB", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }

            // Transparent Fee & Non-Certified Copy Disclosure (Section 2 & Section 7)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate50,
                border = BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Slate600,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "স্বচ্ছতা বিজ্ঞপ্তি: LRgetBD অ্যাপটি ডাউনলোড ও ব্যবহারের জন্য সম্পূর্ণ বিনামূল্যে (কোনো সাবস্ক্রিপশন নেই)। তবে সরকারি সার্টিফাইড কপি বা পোর্টালের নির্ধারিত সরকারি ফি প্রযোজ্য হতে পারে। আনঅফিসিয়াল কপি সরকারি সার্টিফাইড দলিল নয়।",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 10.sp,
                            color = Slate600
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun GeoMetricChip(
    label: String,
    value: String,
    accentColor: Color,
    containerColor: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = accentColor
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = Slate700,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

/**
 * Step 5 Pre-Queue Confirmation Summary Dialog (Section 4, Step 5).
 * Shows real counts before creating the 7-Mouza batch queue.
 */
@Composable
fun PreQueueConfirmationSummaryDialog(
    summary: GeographyCoverageSummary,
    batchSize: Int,
    retrievalMode: RetrievalMode,
    onConfirmCreateQueue: () -> Unit,
    onDismiss: () -> Unit
) {
    val totalBatches = if (summary.selectedMouzasCount == 0) 0 else (summary.selectedMouzasCount + batchSize - 1) / batchSize

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = EmeraldPrimary)
                Text(
                    text = "Confirm Download Queue Summary",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "কিউ তৈরির পূর্বে নির্বাচিত ভৌগোলিক সারসংক্ষেপ যাচাই করুন:",
                    style = MaterialTheme.typography.bodySmall.copy(color = Slate700)
                )
                SummaryRow("Total Districts Selected", "${summary.selectedDistrictsCount}")
                SummaryRow("Total Upazilas Selected", "${summary.selectedUpazilasCount}")
                SummaryRow("Total Mouzas Found", "${summary.totalMouzasFoundInSelection}")
                SummaryRow("Total Mouzas Selected (Duplicate-Free)", "${summary.selectedMouzasCount}")
                SummaryRow("Mouzas with Available Survey Records", "${summary.mouzasWithAvailableSurveys}")
                SummaryRow("Mouzas with Unknown Availability", "${summary.mouzasWithUnknownAvailability}")
                SummaryRow("Mouzas Supporting Manual Retrieval", "${summary.mouzasRequiringManualRetrieval}")
                SummaryRow("7-Mouza Batches to Generate", "$totalBatches Batches ($batchSize Mouzas/Batch)")
                SummaryRow("Retrieval Mode", retrievalMode.labelEn)
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmCreateQueue,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("confirm_create_queue_dialog_button")
            ) {
                Text("Generate $totalBatches Batches & Start Queue", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate50, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = Slate700))
        Text(value, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, color = Slate900))
    }
}

/**
 * Genuine PDF Import & SHA-256 Verification Dialog (Section 8).
 * Lets the user pick a real PDF via Android SAF or test with a valid binary PDF 1.4 sample.
 */
@Composable
fun PdfImportAndVerificationDialog(
    lastStatusMessage: String?,
    onImportPdfBytes: (ByteArray, String, String, String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var khatianNo by remember { mutableStateOf("") }
    var dagNo by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var guardianName by remember { mutableStateOf("") }
    var surveyType by remember { mutableStateOf("RS") }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "imported_record.pdf"
                    onImportPdfBytes(bytes, fileName, khatianNo, dagNo, ownerName, guardianName, surveyType)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = EmeraldPrimary)
                Text("Import & Verify PDF Document", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ব্রাউজার থেকে ডাউনলোডকৃত আসল PDF খতিয়ান/ম্যাপ ফাইল সিলেক্ট করুন। অ্যাপটি স্বয়ংক্রিয়ভাবে PDF স্ট্রাকচার (%PDF Header/EOF) ও SHA-256 চেকসাম যাচাই করে মৌজা ফোল্ডারে সংরক্ষণ করবে।",
                    style = MaterialTheme.typography.bodySmall.copy(color = Slate700)
                )
                OutlinedTextField(
                    value = khatianNo,
                    onValueChange = { khatianNo = it },
                    label = { Text("Khatian Number (খতিয়ান নং)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dagNo,
                    onValueChange = { dagNo = it },
                    label = { Text("Dag Number (দাগ নং)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Owner Name (মালিকের নাম)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = guardianName,
                    onValueChange = { guardianName = it },
                    label = { Text("Guardian Name (পিতা/স্বামীর নাম)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { pdfPickerLauncher.launch("application/pdf") },
                    colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Select PDF File from Device Storage")
                }

                FilledTonalButton(
                    onClick = {
                        val validPdf = DocumentVerificationEngine.generateValidPdfReportBytes(
                            title = "Imported Land Record Reference - Khatian $khatianNo",
                            lines = listOf(
                                "Survey: $surveyType | Khatian: $khatianNo | Dag: $dagNo",
                                "Owner: $ownerName | Guardian: $guardianName"
                            )
                        )
                        onImportPdfBytes(
                            validPdf,
                            "Verified_Import_${System.currentTimeMillis() % 10000}.pdf",
                            khatianNo.ifBlank { "101" },
                            dagNo.ifBlank { "205" },
                            ownerName,
                            guardianName,
                            surveyType
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("verify_sample_pdf_import_button")
                ) {
                    Text("Verify & Archive Valid PDF-1.4 Record")
                }

                if (!lastStatusMessage.isNullOrBlank()) {
                    Surface(
                        color = EmeraldContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = lastStatusMessage,
                            style = MaterialTheme.typography.labelSmall.copy(color = EmeraldDark, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * Geographic Database Runtime Update Dialog (Section 3.2 & Section 10 #17).
 * Allows importing new verified Mouzas via JSON without an APK rebuild.
 */
@Composable
fun GeographicDatabaseUpdateDialog(
    importHistory: List<GeographyImportHistoryEntity>,
    lastStatusMessage: String?,
    onImportJson: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var jsonInput by remember {
        mutableStateOf(
            """
            {
              "source_name": "BBS / DLR&S Supplementary Gazetteer",
              "source_url": "https://bbs.gov.bd",
              "dataset_version": "2026.1-Supp",
              "mouzas": [
                {
                  "upazila_id": "debidwar",
                  "name_en": "Barera",
                  "name_bn": "বড়েরা",
                  "jl_number": "137",
                  "verified": true
                }
              ]
            }
            """.trimIndent()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Map, contentDescription = null, tint = EmeraldPrimary)
                Text("Geographic Database Update", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "APK পুনরায় বিল্ড না করেই নতুন মৌজা বা ভৌগোলিক ডেটাসেট (JSON) ইমপোর্ট ও আপডেট করুন। ডুপ্লিকেট মৌজা ও ত্রুটিপূর্ণ JL নম্বর স্বয়ংক্রিয়ভাবে যাচাই করা হয়।",
                    style = MaterialTheme.typography.bodySmall.copy(color = Slate700)
                )
                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = { jsonInput = it },
                    label = { Text("Geography Update JSON Payload") },
                    minLines = 5,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!lastStatusMessage.isNullOrBlank()) {
                    Text(
                        text = lastStatusMessage,
                        style = MaterialTheme.typography.labelSmall.copy(color = EmeraldDark, fontWeight = FontWeight.Bold)
                    )
                }
                if (importHistory.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = "Recent Import Provenance Log (${importHistory.size}):",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    importHistory.take(3).forEach { item ->
                        Text(
                            text = "• ${item.sourceName} [${item.datasetVersion}]: ${item.recordCount} records (${item.importErrors})",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = Slate600)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onImportJson(jsonInput) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("apply_geo_json_update_button")
            ) {
                Text("Validate & Import Dataset", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
