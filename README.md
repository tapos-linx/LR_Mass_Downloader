# 📜 LRgetBD (ভূমি রেকর্ড ও মৌজা আর্কাইভ গণ-ডাউনলোডার)

[![Android Version](https://img.shields.io/badge/Android-7.0%2B%20(API%2024%2B)-047857?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20MD3-064E3B?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Build Status](https://img.shields.io/badge/Build-Signed%20Release%20APK-brightgreen?style=for-the-badge&logo=githubactions&logoColor=white)](https://github.com/tapos-linx/LR_Mass_Downloader/actions)
[![License](https://img.shields.io/badge/License-MIT%20(100%25%20Free)-blue?style=for-the-badge)](LICENSE)

> **Public, 100% Free, Local-First Bangladesh Geographical Land-Record Search, Queue & Archive Application.**  
> Supports all **8 Administrative Divisions** and all **64 Districts of Bangladesh**, normalized Room geographical tables (`divisions`, `districts`, `upazilas`, `mouzas`, `mouza_aliases`, `mouza_jl_numbers`, `mouza_survey_availability`, `geography_data_sources`, `geography_import_history`), automatic **District → Upazila → All Mouza Selection**, **7-Mouza Batch Queue Generation**, cryptographic **SHA-256 & PDF structure verification**, and **Master Land Ledger** exports (`CSV`, `JSON`, `XLSX`, `PDF`, `TXT`).

---

## 📥 Direct APK Download & Installation

**LRgetBD is 100% free to download, install, and use.** There are no subscriptions, premium locks, or paywalls.

| Asset | Stable Download Link | Description |
| :--- | :--- | :--- |
| 🚀 **Production Signed APK (`LRgetBD.apk`)** | [⬇️ **Download LRgetBD.apk**](https://github.com/tapos-linx/LR_Mass_Downloader/releases/latest/download/LRgetBD.apk) | Standardized signed production release APK built and verified by GitHub Actions. |
| 🔐 **SHA-256 Checksum (`LRgetBD.apk.sha256`)** | [⬇️ **Download LRgetBD.apk.sha256**](https://github.com/tapos-linx/LR_Mass_Downloader/releases/latest/download/LRgetBD.apk.sha256) | Cryptographic SHA-256 checksum for verifying the downloaded APK integrity. |
| 📦 **All Releases** | [📂 **View GitHub Releases**](https://github.com/tapos-linx/LR_Mass_Downloader/releases) | Browse all release versions and release notes. |

### Installation Instructions
1. Download **`LRgetBD.apk`** from the link above (or via the in-app **APK** dialog).
2. Open `LRgetBD.apk` on your Android device (Android 7.0 / API 24 or higher).
3. If prompted for first-time installation from your browser/file manager, enable **Allow from this source**.
4. Verify SHA-256 if desired: `sha256sum -c LRgetBD.apk.sha256`.

---

## ⚖️ Fee Transparency & Official Document Disclosure

- **Free Application:** LRgetBD itself is completely free and open-source.
- **Government & Portal Fees:** Certain official government applications, certified Khatian/Porcha copies, Mouza maps, or third-party portal services may require official government fees paid directly on those portals.
- **Non-Certified Local Copies:** Local archive index files, OCR outputs, or unofficial copies stored in LRgetBD are **not** certified government documents. For official legal transactions, always obtain a certified copy from the official Bangladesh Ministry of Land portal (`https://land.gov.bd` / `https://eporcha.gov.bd`).
- **Website Terms Compliance:** LRgetBD strictly respects third-party terms of service (including `eporcha.tech` and official portals). It never bypasses CAPTCHAs, authentication, payments, or rate limits. When human interaction is required, use **Mode B (User-Assisted Browser & PDF Import)** to browse legally and import your downloaded PDF into the structured Mouza archive.

---

## 🏛️ Complete Bangladesh Geographical Database Architecture

The geographical hierarchy is stored in normalized Room SQLite tables with full indexing:

`Division (8) → District (64) → Upazila/Thana → Mouza → JL Number → Survey Type`

### Normalized Database Tables
1. `divisions`: All 8 Divisions (`Dhaka`, `Chattogram`, `Rajshahi`, `Khulna`, `Barishal`, `Sylhet`, `Rangpur`, `Mymensingh`) with official BBS GeoCodes.
2. `districts`: All 64 Districts of Bangladesh mapped to their Division and official BBS GeoCode.
3. `upazilas`: Verified Upazilas/Thanas across all 64 Districts (including full 17 Upazilas of Cumilla and 9 Upazilas of Brahmanbaria).
4. `mouzas`: Scoped by `upazila_id`, `jl_number`, `official_mouza_code`, `source_reference`, and `verification_status` (`VERIFIED` vs `COMMUNITY_CONTRIBUTED` / `UNVERIFIED`).
5. `mouza_aliases`: Alternative spellings, Bengali/English names, and historical names.
6. `mouza_jl_numbers`: Survey-specific JL numbers (`CS`, `SA`, `RS`, `BS_BRS`).
7. `mouza_survey_availability`: Per-Mouza survey availability status (`AVAILABLE`, `MANUAL_RETRIEVAL_REQUIRED`, `UNKNOWN`, `UNAVAILABLE`).
8. `geography_data_sources`: Provenance metadata (`source_name`, `source_url`, `dataset_version`, `record_count`).
9. `geography_import_history`: Complete audit trail of initial seeds and runtime JSON dataset updates without requiring an APK rebuild.

---

## ⚡ Intelligent 7-Mouza Batch Queue & Document Verification

- **Automatic District → Upazila → All Mouza Selection:** Select a Division and District, click **Select All Upazilas**, and all verified Mouzas are loaded and selected automatically without manual one-by-one clicking.
- **7-Mouza Batching:** Selected Mouzas are deduplicated and partitioned into dynamic batches of **7 Mouzas per batch** (`Batch 1: Mouza 1–7`, `Batch 2: Mouza 8–14`, etc.).
- **Dual Retrieval Modes:**
  - **Mode A (Authorized Automated Retrieval):** Processes authorized local/API registry tasks with exponential backoff and persistent Room + WorkManager state.
  - **Mode B (User-Assisted Browser & PDF Import):** Opens the search portal in a browser for human-permitted retrieval and imports downloaded PDFs into the exact Mouza archive folder.
- **Structured Archive Hierarchy:**
  `Download/LandRecords/LRgetBD/<Division>/<District>/<Upazila>/<Mouza_JL>/<SurveyType>/<Category>/`
- **Cryptographic Verification:** Validates PDF binary headers (`%PDF-` ... `%%EOF`), file size, MIME type, and `SHA-256` checksums to prevent corrupted or duplicate files.
- **Master Land Ledger (`master_land_ledger`):** Organizes all downloaded and imported records with exports to **CSV, JSON, XLSX, PDF, and TXT**, while preserving original OCR text alongside user corrections.

---

## 🧪 Running Tests & Building from Source

```bash
# Run Unit & Robolectric QA Suite
gradle :app:testDebugUnitTest

# Build Debug APK
gradle :app:assembleDebug

# Build Signed Release APK
gradle :app:assembleRelease
```
