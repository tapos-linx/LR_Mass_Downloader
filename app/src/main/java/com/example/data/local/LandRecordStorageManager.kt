package com.example.data.local

import android.app.DownloadManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.example.data.ledger.MasterLedgerEntryEntity
import com.example.data.model.District
import com.example.data.model.DownloadTask
import com.example.data.model.MasterArchive
import com.example.data.model.Upazila
import com.example.util.DocumentValidationResult
import com.example.util.DocumentVerificationEngine
import java.io.File

data class SavedFileResult(
    val primaryDisplayPath: String,
    val mediaStoreUri: Uri?,
    val isPublicDownloads: Boolean,
    val sha256Checksum: String = "",
    val fileSizeBytes: Long = 0L,
    val validationResult: DocumentValidationResult? = null
)

data class MasterArchiveSaveResult(
    val jsonPath: String,
    val textPath: String,
    val csvPath: String,
    val xlsxPath: String = "",
    val pdfPath: String = "",
    val sha256Checksum: String = "",
    val allFiles: List<String>,
    val filePreview: String,
    val isPublicDownloads: Boolean,
    val primaryDisplayPath: String
)

object LandRecordStorageManager {

    private const val LAND_RECORDS_SUBDIR = "LandRecords"
    private const val LRGETBD_ROOT_SUBDIR = "LRgetBD"

    /**
     * Builds the structured archive path required by Section 8:
     * LRgetBD/<Division>/<District>/<Upazila>/<Mouza_JL>/<SurveyType>/<SubCategory>
     */
    fun buildArchiveSubPath(
        divisionNameEn: String = "Chattogram",
        districtNameEn: String,
        upazilaNameEn: String,
        mouzaNameEn: String,
        jlNo: String,
        surveyType: String = "RS",
        subFolder: String = "Metadata"
    ): String {
        val div = sanitizeSegment(divisionNameEn.ifBlank { "Bangladesh" })
        val dist = sanitizeSegment(districtNameEn.ifBlank { "District" })
        val upa = sanitizeSegment(upazilaNameEn.ifBlank { "Upazila" })
        val mouzaJl = "${sanitizeSegment(mouzaNameEn)}_JL_${sanitizeSegment(jlNo)}"
        val survey = sanitizeSegment(surveyType.ifBlank { "RS" })
        val leaf = sanitizeSegment(subFolder.ifBlank { "Metadata" })
        return "$LAND_RECORDS_SUBDIR/$LRGETBD_ROOT_SUBDIR/$div/$dist/$upa/$mouzaJl/$survey/$leaf"
    }

    private fun sanitizeSegment(raw: String): String {
        return raw.trim().replace("[^a-zA-Z0-9_\\-]".toRegex(), "_").trim('_').ifEmpty { "Record" }
    }

    /**
     * Writes raw bytes to public Download/LandRecords/... while preserving exact original document bytes
     * and computing cryptographic SHA-256 checksum.
     */
    fun saveBytesToLandRecords(
        context: Context,
        fileName: String,
        bytes: ByteArray,
        mimeType: String,
        subDir: String = LAND_RECORDS_SUBDIR
    ): SavedFileResult {
        val relativePath = "Download/$subDir/"
        var resolvedPath = ""
        var resolvedUri: Uri? = null
        var isPublic = false
        val sha256 = DocumentVerificationEngine.calculateSha256(bytes)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val resolver = context.contentResolver
                val projection = arrayOf(MediaStore.MediaColumns._ID)
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
                val selectionArgs = arrayOf(fileName, relativePath)
                resolver.query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                        val existingUri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                        try {
                            resolver.delete(existingUri, null, null)
                        } catch (ignored: Exception) {}
                    }
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri, "wt")?.use { stream ->
                        stream.write(bytes)
                        stream.flush()
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                    resolvedUri = uri
                    resolvedPath = "$relativePath$fileName"
                    isPublic = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetFolder = File(publicDownloads, subDir)
                if (!targetFolder.exists()) {
                    targetFolder.mkdirs()
                }

                val directFile = File(targetFolder, fileName)
                directFile.writeBytes(bytes)
                resolvedPath = directFile.absolutePath
                isPublic = true

                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(directFile.absolutePath),
                    arrayOf(mimeType),
                    null
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (resolvedPath.isEmpty()) {
            try {
                val appDownloads = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), subDir)
                if (!appDownloads.exists()) appDownloads.mkdirs()
                val fallbackFile = File(appDownloads, fileName)
                fallbackFile.writeBytes(bytes)
                resolvedPath = fallbackFile.absolutePath
                isPublic = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return SavedFileResult(
            primaryDisplayPath = if (resolvedPath.isNotEmpty()) resolvedPath else "$relativePath$fileName",
            mediaStoreUri = resolvedUri,
            isPublicDownloads = isPublic,
            sha256Checksum = sha256,
            fileSizeBytes = bytes.size.toLong()
        )
    }

    /**
     * Writes UTF-8 text content to Download/LandRecords/... and computes its SHA-256 checksum.
     */
    fun saveFileToLandRecords(
        context: Context,
        fileName: String,
        content: String,
        mimeType: String,
        subDir: String = LAND_RECORDS_SUBDIR
    ): SavedFileResult {
        val bytes = content.toByteArray(Charsets.UTF_8)
        return saveBytesToLandRecords(context, fileName, bytes, mimeType, subDir)
    }

    /**
     * Validates and imports a genuine user-supplied PDF document into the structured Mouza archive folder:
     * LRgetBD/<Division>/<District>/<Upazila>/<Mouza_JL>/<SurveyType>/Imported/
     *
     * Never modifies the original PDF bytes. Rejects invalid/corrupted PDFs.
     */
    fun importAndVerifyUserPdf(
        context: Context,
        pdfBytes: ByteArray,
        originalFileName: String,
        divisionNameEn: String,
        districtNameEn: String,
        upazilaNameEn: String,
        mouzaNameEn: String,
        jlNo: String,
        surveyType: String
    ): SavedFileResult {
        val validation = DocumentVerificationEngine.verifyDocumentBytes(
            bytes = pdfBytes,
            declaredMimeType = "application/pdf",
            fileName = originalFileName
        )

        if (!validation.isValid) {
            return SavedFileResult(
                primaryDisplayPath = "",
                mediaStoreUri = null,
                isPublicDownloads = false,
                sha256Checksum = validation.sha256Checksum,
                fileSizeBytes = validation.fileSizeBytes,
                validationResult = validation
            )
        }

        val subDir = buildArchiveSubPath(
            divisionNameEn = divisionNameEn,
            districtNameEn = districtNameEn,
            upazilaNameEn = upazilaNameEn,
            mouzaNameEn = mouzaNameEn,
            jlNo = jlNo,
            surveyType = surveyType,
            subFolder = "Imported"
        )
        val cleanName = if (originalFileName.endsWith(".pdf", ignoreCase = true)) {
            originalFileName
        } else {
            "$originalFileName.pdf"
        }

        val saved = saveBytesToLandRecords(
            context = context,
            fileName = cleanName,
            bytes = pdfBytes,
            mimeType = "application/pdf",
            subDir = subDir
        )
        return saved.copy(validationResult = validation)
    }

    /**
     * Saves a Mouza Geographic & Provenance Metadata Manifest and a valid PDF 1.4 Archive Index Sheet
     * under Download/LandRecords/LRgetBD/<Division>/<District>/<Upazila>/<Mouza_JL>/<Survey>/Metadata/
     *
     * Never claims to be a certified government Khatian copy.
     */
    fun saveMouzaRecordFile(
        context: Context,
        task: DownloadTask,
        district: District,
        upazila: Upazila
    ): SavedFileResult {
        val safeMouzaName = sanitizeSegment(task.mouzaNameEn)
        val safeUpazilaName = sanitizeSegment(upazila.nameEn)
        val safeRecordType = sanitizeSegment(task.recordTypeLabel)
        val subDir = buildArchiveSubPath(
            divisionNameEn = task.divisionId.replaceFirstChar { it.uppercase() },
            districtNameEn = district.nameEn,
            upazilaNameEn = upazila.nameEn,
            mouzaNameEn = task.mouzaNameEn,
            jlNo = task.jlNo,
            surveyType = task.surveyType,
            subFolder = "Metadata"
        )
        val refSuffix = if (task.referenceId != null) "_Ref_${sanitizeSegment(task.referenceId)}" else ""

        // 1. Generate valid binary PDF 1.4 Local Archive Index Sheet (with explicit non-certified disclosure)
        val pdfFileName = "${safeUpazilaName}_${safeMouzaName}${refSuffix}_JL${task.jlNo}_Archive_Index.pdf"
        val pdfBytes = DocumentVerificationEngine.generateValidPdfReportBytes(
            title = "LRgetBD Local Archive Index - ${district.nameEn} / ${upazila.nameEn} / ${task.mouzaNameEn}",
            lines = listOf(
                "Division: ${task.divisionId.uppercase()} | District: ${district.nameEn} | Upazila: ${upazila.nameEn}",
                "Mouza: ${task.mouzaNameEn} (JL No: ${task.jlNo}) | Mouza ID: ${task.mouzaId}",
                "Survey Categories: ${task.recordTypeLabel}",
                "Retrieval Mode: ${task.retrievalMode.labelEn}",
                "Source Provenance: ${task.sourceName} (${task.sourceReference})",
                "Reference ID: ${task.referenceId ?: "Mouza-Level Registry"}",
                "Timestamp: ${task.timestamp}",
                "Notice: This is a local geographical index sheet generated by LRgetBD.",
                "To attach an official Khatian PDF, use the User-Assisted Browser or PDF Import tool."
            )
        )
        val pdfSaved = saveBytesToLandRecords(context, pdfFileName, pdfBytes, "application/pdf", subDir)

        // 2. Save JSON provenance metadata with the real SHA-256 of the index PDF
        val jsonFileName = "${safeUpazilaName}_${safeMouzaName}${refSuffix}_JL${task.jlNo}_${safeRecordType}.json"
        val jsonContent = """
{
  "application": "LRgetBD - Bangladesh Land-Record Search & Archive",
  "disclaimer": "Unofficial local archive metadata. Not a certified government document.",
  "division": "${task.divisionNameBn} (${task.divisionId})",
  "district": "${district.nameBn} (${district.nameEn})",
  "upazila": "${upazila.nameBn} (${upazila.nameEn})",
  "mouza": {
    "nameBn": "${task.mouzaNameBn}",
    "nameEn": "${task.mouzaNameEn}",
    "jlNo": "${task.jlNo}",
    "mouzaId": "${task.mouzaId}"
  },
  "referenceId": "${task.referenceId ?: "N/A"}",
  "surveyType": "${task.surveyType}",
  "recordCategories": "${task.recordTypeLabel}",
  "retrievalMode": "${task.retrievalMode.name}",
  "sourceName": "${task.sourceName}",
  "sourceReference": "${task.sourceReference}",
  "archiveIndexPdfPath": "${pdfSaved.primaryDisplayPath}",
  "sha256Checksum": "${pdfSaved.sha256Checksum}",
  "fileSizeBytes": ${pdfSaved.fileSizeBytes},
  "createdTimestamp": ${task.timestamp},
  "storageFolder": "Download/$subDir/"
}
""".trimIndent()

        // 3. Save human-readable Bengali/English text summary
        val textFileName = "${safeUpazilaName}_${safeMouzaName}_JL${task.jlNo}_${safeRecordType}.txt"
        val textContent = """
====================================================
  LRgetBD - বাংলাদেশ ভূমি রেকর্ড ও মৌজা আর্কাইভ ইনডেক্স
  (Unofficial Local Archive Index — Not Certified Copy)
====================================================
বিভাগ: ${task.divisionNameBn}
জেলা: ${district.nameBn} (${district.nameEn})
উপজেলা: ${upazila.nameBn} (${upazila.nameEn})
মৌজা: ${task.mouzaNameBn} (${task.mouzaNameEn})
জে.এল নং: ${task.jlNo}
জরিপ ও রেকর্ড ধরন: ${task.recordTypeLabel}
সংগ্রহ পদ্ধতি: ${task.retrievalMode.labelBn}
তথ্যসূত্র: ${task.sourceName} (${task.sourceReference})
SHA-256 চেকসাম: ${pdfSaved.sha256Checksum}
ফাইল আকার: ${pdfSaved.fileSizeBytes} Bytes
অবস্থান: ফোন মেমোরি / Download / $subDir /
====================================================
""".trimIndent()

        saveFileToLandRecords(context, textFileName, textContent, "text/plain", subDir)
        val jsonSaved = saveFileToLandRecords(context, jsonFileName, jsonContent, "application/json", subDir)
        return jsonSaved.copy(
            sha256Checksum = pdfSaved.sha256Checksum,
            fileSizeBytes = pdfSaved.fileSizeBytes
        )
    }

    /**
     * Compiles and exports the Master Land Records Archive & Ledger in all 5 formats required by Section 9:
     * 1. JSON
     * 2. TXT
     * 3. CSV
     * 4. XLSX (SpreadsheetML / Excel compatible)
     * 5. PDF (Valid binary PDF 1.4 report)
     */
    fun saveMasterLandRecordsArchive(
        context: Context,
        archive: MasterArchive,
        district: District,
        upazila: Upazila,
        queueTasks: List<DownloadTask>,
        ledgerEntries: List<MasterLedgerEntryEntity> = emptyList()
    ): MasterArchiveSaveResult {
        val safeUpazilaName = sanitizeSegment(upazila.nameEn)
        val baseName = "${sanitizeSegment(district.nameEn)}_${safeUpazilaName}_Master_Land_Ledger"
        val subDir = "$LAND_RECORDS_SUBDIR/$LRGETBD_ROOT_SUBDIR/${sanitizeSegment(district.divisionId)}/${sanitizeSegment(district.nameEn)}/$safeUpazilaName/Master_Ledger"

        // 1. Master JSON Export
        val jsonBuilder = StringBuilder()
        jsonBuilder.append("{\n")
        jsonBuilder.append("  \"application\": \"LRgetBD Master Land Ledger\",\n")
        jsonBuilder.append("  \"disclaimer\": \"Local archive export. Unofficial copies are not certified government documents.\",\n")
        jsonBuilder.append("  \"district\": \"${district.nameBn} (${district.nameEn})\",\n")
        jsonBuilder.append("  \"upazila\": \"${upazila.nameBn} (${upazila.nameEn})\",\n")
        jsonBuilder.append("  \"recordTypes\": \"${archive.recordTypesIncluded}\",\n")
        jsonBuilder.append("  \"totalMouzasCount\": ${archive.totalMouzasCount},\n")
        jsonBuilder.append("  \"generatedTimestamp\": ${System.currentTimeMillis()},\n")
        jsonBuilder.append("  \"mouzaRecords\": [\n")
        queueTasks.forEachIndexed { i, task ->
            jsonBuilder.append("    {\n")
            jsonBuilder.append("      \"division\": \"${task.divisionNameBn}\",\n")
            jsonBuilder.append("      \"district\": \"${district.nameEn}\",\n")
            jsonBuilder.append("      \"upazila\": \"${upazila.nameEn}\",\n")
            jsonBuilder.append("      \"mouzaId\": \"${task.mouzaId}\",\n")
            jsonBuilder.append("      \"nameBn\": \"${task.mouzaNameBn}\",\n")
            jsonBuilder.append("      \"nameEn\": \"${task.mouzaNameEn}\",\n")
            jsonBuilder.append("      \"jlNo\": \"${task.jlNo}\",\n")
            jsonBuilder.append("      \"surveyType\": \"${task.recordTypeLabel}\",\n")
            jsonBuilder.append("      \"source\": \"${task.sourceName}\",\n")
            jsonBuilder.append("      \"sha256\": \"${task.sha256Checksum}\",\n")
            jsonBuilder.append("      \"status\": \"${task.status.name}\"\n")
            jsonBuilder.append("    }${if (i < queueTasks.size - 1) "," else ""}\n")
        }
        jsonBuilder.append("  ]\n")
        jsonBuilder.append("}\n")
        val jsonContent = jsonBuilder.toString()
        val jsonResult = saveFileToLandRecords(context, "$baseName.json", jsonContent, "application/json", subDir)

        // 2. Master TXT Export
        val textBuilder = StringBuilder()
        textBuilder.append("========================================================================\n")
        textBuilder.append("           LRgetBD — MASTER LAND LEDGER (মাস্টার ল্যান্ড লেজার)\n")
        textBuilder.append("   [Unofficial Archive Ledger — Not a Certified Government Document]\n")
        textBuilder.append("========================================================================\n\n")
        textBuilder.append("জেলা: ${district.nameBn} (${district.nameEn})\n")
        textBuilder.append("উপজেলা: ${upazila.nameBn} (${upazila.nameEn})\n")
        textBuilder.append("জরিপ ও রেকর্ড ধরন: ${archive.recordTypesIncluded}\n")
        textBuilder.append("মোট মৌজা সংখ্যা: ${archive.totalMouzasCount} টি\n")
        textBuilder.append("সংরক্ষণ ফোল্ডার: Download/$subDir/\n\n")
        textBuilder.append("------------------------------------------------------------------------\n")
        textBuilder.append("নং  | মৌজার নাম (বাংলা/ইংরেজি)        | জে.এল নং | জরিপ ধরন | স্ট্যাটাস\n")
        textBuilder.append("------------------------------------------------------------------------\n")
        queueTasks.forEachIndexed { idx, task ->
            val num = String.format("%2d", idx + 1)
            val name = "${task.mouzaNameBn} (${task.mouzaNameEn})".padEnd(28)
            val jl = task.jlNo.padEnd(8)
            val rec = task.recordTypeLabel.padEnd(10)
            textBuilder.append("$num. | $name | $jl | $rec | ${task.status.name}\n")
        }
        textBuilder.append("========================================================================\n")
        val textContent = textBuilder.toString()
        val textResult = saveFileToLandRecords(context, "$baseName.txt", textContent, "text/plain", subDir)

        // 3. Master CSV Export
        val csvBuilder = StringBuilder()
        csvBuilder.append("SL,Division,District,Upazila,Mouza_Bn,Mouza_En,JL_No,Survey_Type,Khatian_No,Dag_No,Owner_Name,Guardian_Name,Source,Verification_Status,SHA256_Checksum,File_Reference\n")
        if (ledgerEntries.isNotEmpty()) {
            ledgerEntries.forEachIndexed { idx, entry ->
                csvBuilder.append(
                    "${idx + 1},\"${entry.divisionName}\",\"${entry.districtName}\",\"${entry.upazilaName}\"," +
                        "\"${entry.mouzaName}\",\"${entry.mouzaName}\",\"${entry.jlNumber}\",\"${entry.surveyType}\"," +
                        "\"${entry.khatianNumber}\",\"${entry.dagNumber}\",\"${entry.ownerName}\",\"${entry.guardianName}\"," +
                        "\"${entry.sourceName}\",\"${entry.verificationStatus.name}\",\"${entry.sha256Checksum}\",\"${entry.fileReference}\"\n"
                )
            }
        } else {
            queueTasks.forEachIndexed { idx, task ->
                csvBuilder.append(
                    "${idx + 1},\"${task.divisionNameBn}\",\"${district.nameEn}\",\"${upazila.nameEn}\"," +
                        "\"${task.mouzaNameBn}\",\"${task.mouzaNameEn}\",\"${task.jlNo}\",\"${task.recordTypeLabel}\"," +
                        "\"${task.referenceId ?: ""}\",\"\",\"\",\"\"," +
                        "\"${task.sourceName}\",\"${task.status.name}\",\"${task.sha256Checksum}\",\"${task.fileLocation}\"\n"
                )
            }
        }
        val csvContent = csvBuilder.toString()
        val csvResult = saveFileToLandRecords(context, "$baseName.csv", csvContent, "text/csv", subDir)

        // 4. Master XLSX (Excel SpreadsheetML) Export
        val xlsxBuilder = StringBuilder()
        xlsxBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        xlsxBuilder.append("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">\n")
        xlsxBuilder.append("  <Worksheet ss:Name=\"MasterLandLedger\">\n    <Table>\n")
        xlsxBuilder.append("      <Row><Cell><Data ss:Type=\"String\">Division</Data></Cell><Cell><Data ss:Type=\"String\">District</Data></Cell><Cell><Data ss:Type=\"String\">Upazila</Data></Cell><Cell><Data ss:Type=\"String\">Mouza</Data></Cell><Cell><Data ss:Type=\"String\">JL_No</Data></Cell><Cell><Data ss:Type=\"String\">Survey_Type</Data></Cell><Cell><Data ss:Type=\"String\">Status</Data></Cell><Cell><Data ss:Type=\"String\">SHA256</Data></Cell></Row>\n")
        queueTasks.forEach { task ->
            xlsxBuilder.append(
                "      <Row>" +
                    "<Cell><Data ss:Type=\"String\">${task.divisionNameBn}</Data></Cell>" +
                    "<Cell><Data ss:Type=\"String\">${district.nameEn}</Data></Cell>" +
                    "<Cell><Data ss:Type=\"String\">${upazila.nameEn}</Data></Cell>" +
                    "<Cell><Data ss:Type=\"String\">${task.mouzaNameEn}</Data></Cell>" +
                    "<Cell><Data ss:Type=\"String\">${task.jlNo}</Data></Cell>" +
                    "<Cell><Data ss:Type=\"String\">${task.recordTypeLabel}</Data></Cell>" +
                    "<Cell><Data ss:Type=\"String\">${task.status.name}</Data></Cell>" +
                    "<Cell><Data ss:Type=\"String\">${task.sha256Checksum}</Data></Cell>" +
                    "</Row>\n"
            )
        }
        xlsxBuilder.append("    </Table>\n  </Worksheet>\n</Workbook>\n")
        val xlsxResult = saveFileToLandRecords(
            context,
            "$baseName.xlsx",
            xlsxBuilder.toString(),
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            subDir
        )

        // 5. Master PDF (Standards-compliant PDF 1.4 binary) Export
        val pdfLines = mutableListOf(
            "District: ${district.nameEn} | Upazila: ${upazila.nameEn} | Total Mouzas: ${archive.totalMouzasCount}",
            "Surveys: ${archive.recordTypesIncluded}"
        )
        queueTasks.take(35).forEachIndexed { idx, task ->
            pdfLines.add("${idx + 1}. ${task.mouzaNameEn} (JL: ${task.jlNo}) - ${task.recordTypeLabel} [${task.status.name}]")
        }
        val pdfBytes = DocumentVerificationEngine.generateValidPdfReportBytes(
            title = "LRgetBD Master Land Ledger - ${district.nameEn} / ${upazila.nameEn}",
            lines = pdfLines
        )
        val pdfResult = saveBytesToLandRecords(context, "$baseName.pdf", pdfBytes, "application/pdf", subDir)

        val filesList = listOf(
            jsonResult.primaryDisplayPath,
            textResult.primaryDisplayPath,
            csvResult.primaryDisplayPath,
            xlsxResult.primaryDisplayPath,
            pdfResult.primaryDisplayPath
        )

        return MasterArchiveSaveResult(
            jsonPath = jsonResult.primaryDisplayPath,
            textPath = textResult.primaryDisplayPath,
            csvPath = csvResult.primaryDisplayPath,
            xlsxPath = xlsxResult.primaryDisplayPath,
            pdfPath = pdfResult.primaryDisplayPath,
            sha256Checksum = pdfResult.sha256Checksum,
            allFiles = filesList,
            filePreview = textContent,
            isPublicDownloads = jsonResult.isPublicDownloads,
            primaryDisplayPath = jsonResult.primaryDisplayPath
        )
    }

    /**
     * Opens the device's Downloads folder directly in the system Files or Downloads app.
     */
    fun openDownloadsFolder(context: Context) {
        var opened = false

        try {
            val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            opened = true
        } catch (ignored: Exception) {}

        if (!opened) {
            try {
                val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val landRecordsFolder = File(publicDownloads, LAND_RECORDS_SUBDIR)
                val target = if (landRecordsFolder.exists()) landRecordsFolder else publicDownloads
                val uri = Uri.parse(target.absolutePath)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "resource/folder")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                opened = true
            } catch (ignored: Exception) {}
        }

        if (!opened) {
            try {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                opened = true
            } catch (ignored: Exception) {}
        }

        if (!opened) {
            Toast.makeText(
                context,
                "Saved to: Download/LandRecords/LRgetBD/\nOpen your 'Files' app to view.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Writes content directly to a user-selected SAF Uri (Storage Access Framework).
     */
    fun writeToSafUri(context: Context, uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
                out.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Cleans temporary cache without touching downloaded records in Download/LandRecords
     */
    fun clearCacheAndMemory(context: Context) {
        try {
            context.cacheDir.deleteRecursively()
            context.externalCacheDir?.deleteRecursively()
        } catch (ignored: Exception) {}
        System.gc()
    }
}
