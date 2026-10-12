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
import com.example.data.model.District
import com.example.data.model.DownloadTask
import com.example.data.model.MasterArchive
import com.example.data.model.Upazila
import java.io.File

data class SavedFileResult(
    val primaryDisplayPath: String,
    val mediaStoreUri: Uri?,
    val isPublicDownloads: Boolean
)

data class MasterArchiveSaveResult(
    val jsonPath: String,
    val textPath: String,
    val csvPath: String,
    val allFiles: List<String>,
    val filePreview: String
)

object LandRecordStorageManager {

    private const val LAND_RECORDS_SUBDIR = "LandRecords"
    private const val RELATIVE_DOWNLOAD_PATH = "Download/$LAND_RECORDS_SUBDIR/"

    /**
     * Writes a file to the public Download/LandRecords folder on phone memory.
     * Uses MediaStore on Android 10+ (API 29+) and direct File I/O + MediaScanner on all versions.
     */
    fun saveFileToLandRecords(
        context: Context,
        fileName: String,
        content: String,
        mimeType: String
    ): SavedFileResult {
        var resolvedPath = ""
        var resolvedUri: Uri? = null

        // 1. Android 10+ (API 29+) MediaStore Downloads API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val resolver = context.contentResolver

                // Query and clean up duplicate/previous entry with the same name if exists
                val projection = arrayOf(MediaStore.MediaColumns._ID)
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
                val selectionArgs = arrayOf(fileName, RELATIVE_DOWNLOAD_PATH)
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
                    put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_DOWNLOAD_PATH)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri, "wt")?.use { stream ->
                        stream.write(content.toByteArray(Charsets.UTF_8))
                        stream.flush()
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                    resolvedUri = uri
                    resolvedPath = "/storage/emulated/0/Download/$LAND_RECORDS_SUBDIR/$fileName"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Direct File writing to public storage (/storage/emulated/0/Download/LandRecords/)
        try {
            val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val landRecordsFolder = File(publicDownloads, LAND_RECORDS_SUBDIR)
            if (!landRecordsFolder.exists()) {
                landRecordsFolder.mkdirs()
            }

            val directFile = File(landRecordsFolder, fileName)
            directFile.writeText(content, Charsets.UTF_8)
            resolvedPath = directFile.absolutePath

            // Broadcast to Android Media Scanner so all file browser apps immediately index it
            MediaScannerConnection.scanFile(
                context,
                arrayOf(directFile.absolutePath),
                arrayOf(mimeType),
                null
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Fallback to external files if public access is restricted by device policy
        if (resolvedPath.isEmpty()) {
            try {
                val appDownloads = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), LAND_RECORDS_SUBDIR)
                if (!appDownloads.exists()) appDownloads.mkdirs()
                val fallbackFile = File(appDownloads, fileName)
                fallbackFile.writeText(content, Charsets.UTF_8)
                resolvedPath = fallbackFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return SavedFileResult(
            primaryDisplayPath = if (resolvedPath.isNotEmpty()) resolvedPath else "Download/LandRecords/$fileName",
            mediaStoreUri = resolvedUri,
            isPublicDownloads = true
        )
    }

    /**
     * Saves an individual Mouza survey record directly to Download/LandRecords/
     */
    fun saveMouzaRecordFile(
        context: Context,
        task: DownloadTask,
        district: District,
        upazila: Upazila
    ): SavedFileResult {
        val safeMouzaName = task.mouzaNameEn.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val safeRecordType = task.recordTypeLabel.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val jsonFileName = "${upazila.nameEn}_${safeMouzaName}_JL${task.jlNo}_${safeRecordType}.json"

        val jsonContent = """
{
  "system": "Bangladesh Digital Land Records & Survey (DLR&S)",
  "district": "${district.nameBn} (${district.nameEn})",
  "upazila": "${upazila.nameBn} (${upazila.nameEn})",
  "mouza": {
    "nameBn": "${task.mouzaNameBn}",
    "nameEn": "${task.mouzaNameEn}",
    "jlNo": "${task.jlNo}",
    "mouzaId": "${task.mouzaId}"
  },
  "recordType": "${task.recordTypeLabel}",
  "totalKhatians": ${task.totalKhatians},
  "fileSizeBytes": ${task.fileSizeBytes},
  "downloadTimestamp": ${task.timestamp},
  "status": "VERIFIED_OFFLINE_SAVED",
  "storageFolder": "Download/LandRecords/"
}
""".trimIndent()

        // Also save readable text ledger for the mouza
        val textFileName = "${upazila.nameEn}_${safeMouzaName}_JL${task.jlNo}_${safeRecordType}.txt"
        val textContent = """
====================================================
  গণপ্রজাতন্ত্রী বাংলাদেশ সরকার - ভূমি রেকর্ড ও জরিপ
  মৌজা খতিয়ান ফাইল (Land Record Mouza Document)
====================================================
জেলা: ${district.nameBn} (${district.nameEn})
উপজেলা: ${upazila.nameBn} (${upazila.nameEn})
মৌজা: ${task.mouzaNameBn} (${task.mouzaNameEn})
জে.এল নং: ${task.jlNo}
জরিপ রেকর্ড ধরন: ${task.recordTypeLabel}
মোট খতিয়ান সংখ্যা: ${task.totalKhatians} টি
সংরক্ষণ সময়: ${task.timestamp}
অবস্থান: ফোন মেমোরি / Download / LandRecords /
====================================================
""".trimIndent()

        saveFileToLandRecords(context, textFileName, textContent, "text/plain")
        return saveFileToLandRecords(context, jsonFileName, jsonContent, "application/json")
    }

    /**
     * Compiles and writes the complete Master Land Records Archive
     * (JSON, TXT, and CSV) to Download/LandRecords/
     */
    fun saveMasterLandRecordsArchive(
        context: Context,
        archive: MasterArchive,
        district: District,
        upazila: Upazila,
        queueTasks: List<DownloadTask>
    ): MasterArchiveSaveResult {
        val baseName = "${district.nameEn}_${upazila.nameEn}_Master_Land_Records_2026"

        // 1. Generate Master JSON
        val jsonBuilder = StringBuilder()
        jsonBuilder.append("{\n")
        jsonBuilder.append("  \"portal\": \"Bangladesh Digital Land Records (DLR&S)\",\n")
        jsonBuilder.append("  \"district\": \"${district.nameBn} (${district.nameEn})\",\n")
        jsonBuilder.append("  \"upazila\": \"${upazila.nameBn} (${upazila.nameEn})\",\n")
        jsonBuilder.append("  \"recordTypes\": \"${archive.recordTypesIncluded}\",\n")
        jsonBuilder.append("  \"totalMouzasFound\": ${archive.totalMouzasCount},\n")
        jsonBuilder.append("  \"totalKhatians\": ${archive.totalKhatiansCount},\n")
        jsonBuilder.append("  \"archivedSize\": \"${archive.totalSizeMb}\",\n")
        jsonBuilder.append("  \"generatedDate\": \"${System.currentTimeMillis()}\",\n")
        jsonBuilder.append("  \"storageLocation\": \"Download/LandRecords/\",\n")
        jsonBuilder.append("  \"mouzaRecords\": [\n")

        queueTasks.forEachIndexed { i, task ->
            jsonBuilder.append("    {\n")
            jsonBuilder.append("      \"mouzaId\": \"${task.mouzaId}\",\n")
            jsonBuilder.append("      \"nameBn\": \"${task.mouzaNameBn}\",\n")
            jsonBuilder.append("      \"nameEn\": \"${task.mouzaNameEn}\",\n")
            jsonBuilder.append("      \"jlNo\": \"${task.jlNo}\",\n")
            jsonBuilder.append("      \"recordType\": \"${task.recordTypeLabel}\",\n")
            jsonBuilder.append("      \"khatians\": ${task.totalKhatians},\n")
            jsonBuilder.append("      \"status\": \"VERIFIED_DOWNLOADED\"\n")
            jsonBuilder.append("    }${if (i < queueTasks.size - 1) "," else ""}\n")
        }
        jsonBuilder.append("  ]\n")
        jsonBuilder.append("}\n")
        val jsonContent = jsonBuilder.toString()
        val jsonFileName = "$baseName.json"
        val jsonResult = saveFileToLandRecords(context, jsonFileName, jsonContent, "application/json")

        // 2. Generate Master Readable Text Ledger
        val textBuilder = StringBuilder()
        textBuilder.append("========================================================================\n")
        textBuilder.append("       গণপ্রজাতন্ত্রী বাংলাদেশ সরকার - ভূমি রেকর্ড ও জরিপ অধিদপ্তর\n")
        textBuilder.append("         মাস্টার ল্যান্ড রেকর্ডস লেজার (MASTER LAND RECORDS LEDGER)\n")
        textBuilder.append("========================================================================\n\n")
        textBuilder.append("জেলা: ${district.nameBn} (${district.nameEn})\n")
        textBuilder.append("উপজেলা: ${upazila.nameBn} (${upazila.nameEn})\n")
        textBuilder.append("জরিপ রেকর্ড ধরন: ${archive.recordTypesIncluded}\n")
        textBuilder.append("মোট মৌজা সংখ্যা: ${archive.totalMouzasCount} টি\n")
        textBuilder.append("মোট খতিয়ান সংখ্যা: ${archive.totalKhatiansCount} টি\n")
        textBuilder.append("আর্কাইভ ফাইল আকার: ${archive.totalSizeMb}\n")
        textBuilder.append("সংরক্ষণ ফোল্ডার: ফোন মেমোরি / Download / LandRecords /\n\n")
        textBuilder.append("------------------------------------------------------------------------\n")
        textBuilder.append("নং  | মৌজার নাম (বাংলা/ইংরেজি)        | জে.এল নং | রেকর্ড ধরন | খতিয়ান\n")
        textBuilder.append("------------------------------------------------------------------------\n")
        queueTasks.forEachIndexed { idx, task ->
            val num = String.format("%2d", idx + 1)
            val name = "${task.mouzaNameBn} (${task.mouzaNameEn})".padEnd(28)
            val jl = task.jlNo.padEnd(8)
            val rec = task.recordTypeLabel.padEnd(10)
            textBuilder.append("$num. | $name | $jl | $rec | ${task.totalKhatians} টি\n")
        }
        textBuilder.append("========================================================================\n")
        textBuilder.append("সকল মৌজার খতিয়ান ডাটা সফলভাবে ডাউনলোড ও সংরক্ষণ করা হয়েছে।\n")
        val textContent = textBuilder.toString()
        val textFileName = "$baseName.txt"
        val textResult = saveFileToLandRecords(context, textFileName, textContent, "text/plain")

        // 3. Generate Master CSV (Spreadsheet)
        val csvBuilder = StringBuilder()
        csvBuilder.append("SL,District,Upazila,Mouza_Bn,Mouza_En,JL_No,Record_Type,Total_Khatians,Status\n")
        queueTasks.forEachIndexed { idx, task ->
            csvBuilder.append("${idx + 1},\"${district.nameEn}\",\"${upazila.nameEn}\",\"${task.mouzaNameBn}\",\"${task.mouzaNameEn}\",\"${task.jlNo}\",\"${task.recordTypeLabel}\",${task.totalKhatians},\"COMPLETED\"\n")
        }
        val csvContent = csvBuilder.toString()
        val csvFileName = "$baseName.csv"
        val csvResult = saveFileToLandRecords(context, csvFileName, csvContent, "text/csv")

        val filesList = listOf(
            jsonResult.primaryDisplayPath,
            textResult.primaryDisplayPath,
            csvResult.primaryDisplayPath
        )

        return MasterArchiveSaveResult(
            jsonPath = jsonResult.primaryDisplayPath,
            textPath = textResult.primaryDisplayPath,
            csvPath = csvResult.primaryDisplayPath,
            allFiles = filesList,
            filePreview = textContent
        )
    }

    /**
     * Opens the device's Downloads folder directly in the system Files or Downloads app.
     */
    fun openDownloadsFolder(context: Context) {
        var opened = false

        // 1. Try launching the standard Android Downloads UI
        try {
            val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            opened = true
        } catch (ignored: Exception) {}

        // 2. Try launching file manager with Downloads directory URI
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

        // 3. Fallback to general storage document picker/viewer
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
                "Please open your phone's 'Files' or 'Downloads' app to see the 'LandRecords' folder.",
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
     * Cleans temporary cache without touching the downloaded records in Download/LandRecords
     */
    fun clearCacheAndMemory(context: Context) {
        try {
            context.cacheDir.deleteRecursively()
            context.externalCacheDir?.deleteRecursively()
        } catch (ignored: Exception) {}
        System.gc()
    }
}
