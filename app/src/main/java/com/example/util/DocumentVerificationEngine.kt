package com.example.util

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

data class DocumentValidationResult(
    val isValid: Boolean,
    val sha256Checksum: String,
    val fileSizeBytes: Long,
    val detectedMimeType: String,
    val isPdfStructureValid: Boolean,
    val errorReason: String? = null
)

/**
 * Genuine Document Verification, PDF Structure Inspector, and SHA-256 Checksum Engine (Section 8).
 *
 * Rules enforced:
 * - Never generates fake Khatian PDFs or marks corrupted files as verified.
 * - Preserves original bytes of user-imported or authorized API documents.
 * - Calculates cryptographic SHA-256 checksums for duplicate detection and ledger provenance.
 */
object DocumentVerificationEngine {

    /**
     * Computes lowercase hexadecimal SHA-256 digest of a byte array.
     */
    fun calculateSha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Computes lowercase hexadecimal SHA-256 digest of a File without loading huge files into RAM at once.
     */
    fun calculateSha256(file: File): String {
        if (!file.exists() || !file.isFile) return ""
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read = input.read(buffer)
            while (read > 0) {
                digest.update(buffer, 0, read)
                read = input.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Validates binary PDF structure according to ISO 32000-1 / PDF 1.x-2.x specification:
     * 1. Minimum byte length (>= 64 bytes)
     * 2. `%PDF-` magic header within first 1024 bytes
     * 3. Contains at least one `obj` and `endobj`
     * 4. Contains `%%EOF` trailer marker near the end of the file
     */
    fun validatePdfStructure(bytes: ByteArray): Boolean {
        if (bytes.size < 64) return false
        val headerWindowSize = minOf(bytes.size, 1024)
        val headerAscii = String(bytes, 0, headerWindowSize, Charsets.ISO_8859_1)
        if (!headerAscii.contains("%PDF-")) return false

        val fullAscii = if (bytes.size <= 65536) {
            String(bytes, Charsets.ISO_8859_1)
        } else {
            val head = String(bytes, 0, 32768, Charsets.ISO_8859_1)
            val tail = String(bytes, bytes.size - 32768, 32768, Charsets.ISO_8859_1)
            head + "\n" + tail
        }

        val hasObjects = fullAscii.contains("obj") && fullAscii.contains("endobj")
        val tailWindowSize = minOf(bytes.size, 2048)
        val tailAscii = String(bytes, bytes.size - tailWindowSize, tailWindowSize, Charsets.ISO_8859_1)
        val hasEof = tailAscii.contains("%%EOF")

        return hasObjects && hasEof
    }

    /**
     * Validates a downloaded or imported document byte array for MIME type, size, PDF structure, and SHA-256.
     */
    fun verifyDocumentBytes(
        bytes: ByteArray,
        declaredMimeType: String = "application/pdf",
        fileName: String = "document.pdf"
    ): DocumentValidationResult {
        if (bytes.isEmpty()) {
            return DocumentValidationResult(
                isValid = false,
                sha256Checksum = "",
                fileSizeBytes = 0L,
                detectedMimeType = declaredMimeType,
                isPdfStructureValid = false,
                errorReason = "Document is empty (0 bytes)"
            )
        }

        val sha256 = calculateSha256(bytes)
        val isPdfExpected = declaredMimeType.equals("application/pdf", ignoreCase = true) ||
            fileName.endsWith(".pdf", ignoreCase = true)

        if (isPdfExpected) {
            val pdfOk = validatePdfStructure(bytes)
            if (!pdfOk) {
                return DocumentValidationResult(
                    isValid = false,
                    sha256Checksum = sha256,
                    fileSizeBytes = bytes.size.toLong(),
                    detectedMimeType = "application/octet-stream",
                    isPdfStructureValid = false,
                    errorReason = "Invalid or corrupted PDF structure (missing %PDF- header, objects, or %%EOF trailer)"
                )
            }
            return DocumentValidationResult(
                isValid = true,
                sha256Checksum = sha256,
                fileSizeBytes = bytes.size.toLong(),
                detectedMimeType = "application/pdf",
                isPdfStructureValid = true,
                errorReason = null
            )
        }

        return DocumentValidationResult(
            isValid = bytes.isNotEmpty(),
            sha256Checksum = sha256,
            fileSizeBytes = bytes.size.toLong(),
            detectedMimeType = declaredMimeType,
            isPdfStructureValid = false,
            errorReason = null
        )
    }

    /**
     * Generates a standards-compliant binary PDF 1.4 document (`%PDF-1.4` ... `%%EOF`)
     * for exporting the Master Land Ledger or Local Archive Index Report.
     *
     * Explicitly states that this PDF is an LRgetBD Archive Index / Ledger Report and NOT
     * an official certified government Khatian copy.
     */
    fun generateValidPdfReportBytes(
        title: String,
        lines: List<String>
    ): ByteArray {
        val sanitizedLines = mutableListOf<String>()
        sanitizedLines.add(escapePdfText(title))
        sanitizedLines.add(escapePdfText("DISCLAIMER: Unofficial Local Archive Ledger Report (Not a Certified Government Document)"))
        sanitizedLines.add(escapePdfText("--------------------------------------------------------------------------------"))
        lines.take(42).forEach { line ->
            sanitizedLines.add(escapePdfText(line))
        }

        val streamBuilder = StringBuilder()
        streamBuilder.append("BT\n/F1 10 Tf\n50 760 Td\n14 TL\n")
        sanitizedLines.forEachIndexed { index, text ->
            if (index == 0) {
                streamBuilder.append("($text) Tj\n")
            } else {
                streamBuilder.append("T* ($text) Tj\n")
            }
        }
        streamBuilder.append("ET\n")
        val contentStream = streamBuilder.toString()
        val streamBytes = contentStream.toByteArray(Charsets.ISO_8859_1)

        val obj1 = "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n"
        val obj2 = "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n"
        val obj3 = "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n"
        val obj4 = "4 0 obj\n<< /Length ${streamBytes.size} >>\nstream\n${contentStream}endstream\nendobj\n"
        val obj5 = "5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n"

        val header = "%PDF-1.4\n"
        val offset1 = header.length
        val offset2 = offset1 + obj1.length
        val offset3 = offset2 + obj2.length
        val offset4 = offset3 + obj3.length
        val offset5 = offset4 + obj4.length
        val xrefOffset = offset5 + obj5.length

        val xref = buildString {
            append("xref\n0 6\n")
            append("0000000000 65535 f \n")
            append("%010d 00000 n \n".format(offset1))
            append("%010d 00000 n \n".format(offset2))
            append("%010d 00000 n \n".format(offset3))
            append("%010d 00000 n \n".format(offset4))
            append("%010d 00000 n \n".format(offset5))
            append("trailer\n<< /Size 6 /Root 1 0 R >>\n")
            append("startxref\n$xrefOffset\n%%EOF\n")
        }

        val fullPdf = header + obj1 + obj2 + obj3 + obj4 + obj5 + xref
        return fullPdf.toByteArray(Charsets.ISO_8859_1)
    }

    private fun escapePdfText(input: String): String {
        val asciiOnly = input.map { ch ->
            if (ch.code in 32..126) ch else '?'
        }.joinToString("")
        return asciiOnly
            .replace("\\", "\\\\")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .take(95)
    }
}
