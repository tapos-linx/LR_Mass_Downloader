# 📜 LR get BD (ভূমি রেকর্ড ও জরিপ গণ-ডাউনলোডার)

[![Android Version](https://img.shields.io/badge/Android-7.0%2B%20(API%2024%2B)-047857?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20MD3-064E3B?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Build Status](https://img.shields.io/badge/Build-Auto--Release-brightgreen?style=for-the-badge&logo=githubactions&logoColor=white)](https://github.com/tapos-linx/LR_Mass_Downloader/actions)
[![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](LICENSE)

> **Specialized GovTech Land Records Mass Downloader for Bangladesh Land Records & Survey (DLR&S).**  
> Covers **Cumilla (কুমিল্লা - 17 Upazilas)** and **Brahmanbaria (ব্রাহ্মণবাড়িয়া - 9 Upazilas)** with sequential anti-freeze queue, automated 7-mouza batching, master ledger synthesis, direct phone storage integration (`Download/LandRecords/`), and automated cache clearing.

---

## 📥 How to Get & Install the APK File

### Option 1: Direct Download from AI Studio Build (Instant — 0 Seconds)
If you are developing or testing this project in **AI Studio**:
1. Tap the **Settings** / **Export** menu in AI Studio.
2. Select **"Download APK"** or **"Generate APK"**.
3. Your device or browser will download the compiled `app-debug.apk` immediately without waiting for GitHub.

### Option 2: In-App Direct 1-Click APK Download & Install (Zero Share Sheet)
Inside the running Android app:
1. Tap the **"APK"** button in the top GovTech header bar.
2. Tap **"Downloads এ সেভ" (Save to Downloads)** — The APK is saved immediately to `/sdcard/Download/LRMassDownloader.apk` with zero internet cost and zero share sheets!
3. Or tap **"সরাসরি ইনস্টল" (Direct Install)** — Android's package installer launches immediately to install the APK!

### Option 3: GitHub Releases Direct Production Downloads (Official URL Pattern)

| Download Type | Direct Link | Description |
| :--- | :--- | :--- |
| 🚀 **Direct Production Download (Official Pattern)** | [⬇️ **Download LR Mass Downloader.apk**](https://github.com/tapos-linx/LR_Mass_Downloader/releases/latest/download/LR%20Mass%20Downloader.apk) | Official direct asset download pattern: `releases/latest/download/<LR Mass Downloader>.apk`. Initiates instant browser download without 404! |
| 🚀 **Direct Download (Unspaced Alias)** | [⬇️ **Download LRMassDownloader.apk**](https://github.com/tapos-linx/LR_Mass_Downloader/releases/latest/download/LRMassDownloader.apk) | Direct production asset download without spaces. |
| 🚀 **Direct Download (Underscored)** | [⬇️ **Download LR_Mass_Downloader.apk**](https://github.com/tapos-linx/LR_Mass_Downloader/releases/latest/download/LR_Mass_Downloader.apk) | Direct release asset matching repository slug. |
| 📦 **All Releases & Versions** | [📂 **View All Releases & Version History**](https://github.com/tapos-linx/LR_Mass_Downloader/releases) | Browse all release tags and historical builds. |
| ⚙️ **Live Build Status** | [📊 **GitHub Actions Live Runs**](https://github.com/tapos-linx/LR_Mass_Downloader/actions) | Monitor the automated CI/CD pipeline. |

---

## 💡 Why Does GitHub Return a "404 Not Found" Error?

If you see a `404 Not Found` error when opening a GitHub APK download link, it is due to one of three common causes:

1. **Repository Has Not Been Pushed to GitHub Yet**:
   - The remote repository `tapos-linx/LR_Mass_Downloader` has not yet been pushed to your GitHub account.
   - **Solution**: Follow the [One-Command Push Guide](#-connecting-to-github--auto-release) below to push your code to your GitHub account (`git push -u origin main --tags`). Once pushed, the repository is created and GitHub Actions triggers automatically.
2. **Repository Name or Account Mismatch**:
   - If your GitHub username is different or your repository is named differently (e.g. `lr-mass-downloader` in lowercase), any hardcoded link to `tapos-linx/LR_Mass_Downloader` returns `404`.
   - **Solution**: In the app's **APK Dialog**, tap **"Change Repo"** and enter your exact `username/repository-name` to generate valid links for your repo!
3. **GitHub Actions Build Still in Progress**:
   - After pushing code, GitHub Actions takes **1 to 2 minutes** to compile the Android app and upload the APK to GitHub Releases.
   - Before the build completes, the release asset does not exist yet and GitHub returns `404`.
   - **Solution**: Visit your repository's [GitHub Actions Tab](../../actions) and wait for the green checkmark (✅). As soon as the build finishes, the download link is live!

---

## ⚡ Fast Installation Guide (Fix for Slow Installs)

If APK installation from GitHub ever takes too long or pauses on your phone:

1. **Tap the Download Link**: The file `LRMassDownloader.apk` will download to your `Downloads` folder.
2. **Open the File**: Tap **Open** from the download notification or locate it in your **Files / My Files** app.
3. **Allow Installation from Source** *(First-time only)*:
   - If prompted: *"For your security, your phone is not allowed to install unknown apps from this source"*, tap **Settings** ➔ toggle **Allow from this source** on.
4. **Bypass Play Protect Wait**:
   - If Google Play Protect displays a prompt: *"Unrecognized app: Install anyway?"*, tap **"Install anyway"**.
   - ⚠️ *Note:* If you wait for Play Protect to send the app to the cloud for deep analysis, it can pause for 30–60 seconds. Tapping **"Install anyway"** installs the application in **1 to 2 seconds**.
5. **Ready!** Open **LR Mass Downloader** from your app drawer.

---

## 🔄 Automated Versioning & GitHub Sync System

This project features an automated versioning and release system:

### How it works:
1. **Dynamic Versioning**:
   - The app's version is configured in `version.properties` and dynamically calculated in `app/build.gradle.kts`.
   - In GitHub Actions CI/CD (`.github/workflows/build-apk.yml`), every push automatically increments the version (e.g. `1.0.1`, `1.0.2`, `1.0.3`...) based on `github.run_number`.
   - A unique Git tag (e.g. `v1.0.4`) and a GitHub Release are automatically generated and marked as `latest`.
2. **In-App Version Indicator**:
   - The app UI displays its exact active version: e.g. `Installed: v1.0.3 (Build 103)` in the **APK Dialog** and **GovTech Header**.
3. **One-Command Auto Version Bump & Push**:
   You can run the included helper script in terminal:
   ```bash
   ./scripts/auto_commit_and_push.sh "Your update message"
   ```
   This will:
   - Automatically increment the build number and patch version in `version.properties`.
   - Commit all pending files.
   - Create a local version tag.
   - Push to your remote `origin` on GitHub.
   - Trigger the GitHub Actions workflow to publish the new APK release automatically!

---

## ✨ Key Features & Workflow

### 1. 🏛️ Pure Cascading Hierarchy & Complete Dataset
- **Cumilla District (কুমিল্লা - 17 Upazilas)**: Adarsha Sadar, Sadar Dakshin, Chandina, Daudkandi, Debidwar, Burichang, Brahmanpara, Chauddagram, Laksam, Muradnagar, Barura, Homna, Titas, Meghna, Monohargonj, Nangalkot, Lalmai.
- **Brahmanbaria District (ব্রাহ্মণবাড়িয়া - 9 Upazilas)**: Sadar, Sarail, Ashuganj, Kasba, Nabinagar, Nasirnagar, Bancharampur, Akhaura, Bijoynagar.
- Selecting a District resets and dynamically loads Upazilas.
- Selecting an Upazila immediately queries and displays the total verified mouza count.

### 2. ⚡ Automated 7-Mouza Sequential Batch Queue (Anti-Repeat)
- **Automatic 7-Mouza Pipeline**: Automatically selects the first 7 mouzas when an Upazila is chosen.
- **Sequential Auto-Progression**: Once a 7-mouza batch completes, the app **automatically queues the next 7 mouzas** that have not been downloaded yet.
- **Strict Anti-Duplication**: Prevents previously downloaded mouzas from repeating or downloading in infinite loops.
- **Auto-Completion**: Automatically compiles the Master Archive once all mouzas in the Upazila are finished.

### 3. 📑 One-Click "Select All Documents"
- Support for all four standard Bangladesh Land Survey record types:
  - **CS (Cadastral Survey / সি.এস)**
  - **SA (State Acquisition / এস.এ)**
  - **RS (Revisional Survey / আর.এস)**
  - **BRS / BS (Bangladesh Survey / বি.আর.এস / বি.এস)**
- "Select All" toggle quickly selects or deselects all four record types simultaneously.

### 4. 🛡️ Background WorkManager Service & Anti-Blocking Protection
- **Jetpack WorkManager Background Engine**: Runs asynchronously via `DownloadQueueWorker` (tied to `FOREGROUND_SERVICE_DATA_SYNC` and persistent status notifications) so downloads proceed smoothly even if the app is backgrounded or the screen is turned off.
- **Randomized Jitter Delays (Anti-Blocking)**: Government servers (e-Porcha / DLR&S) detect rapid bot queries. The service injects randomized delays (400ms – 2,400ms jitter between request batches and cooldowns between mouzas) to prevent server-side 429 rate limits and IP blocking.
- **100% Fluid & Responsive UI**: UI state updates reactively via Room database Flow listeners, guaranteeing zero dropped frames or UI thread freezes.
- **Direct User Controls**: Pause/Resume buttons and a dedicated WorkManager toggle card in the Queue HUD.

### 5. 📁 Master Archive Synthesis & Phone Storage
- Exports **Master JSON**, **Human-Readable TXT Ledger**, and **CSV Table**.
- Directly stored in public device memory: `Download/LandRecords/<District>/<Upazila>/`.
- Automatically scanned via Android `MediaScannerConnection` so files appear instantly in Samsung My Files, Google Files, and desktop MTP connections.
- **Auto-Clears Cache**: Automatically purges temporary download chunks after master file generation to conserve phone storage.
- Storage Access Framework (SAF) fallback for external SD cards.

---

## 🛠️ Tech Stack & Architecture

| Layer | Technology |
| :--- | :--- |
| **Language** | Kotlin 2.0+ |
| **UI Framework** | Jetpack Compose with Material Design 3 (Emerald/Amber GovTech palette) |
| **Architecture** | MVVM with `StateFlow` and unidirectional data flow |
| **Concurrency** | Kotlin Coroutines, Flow, and Mutex Concurrency Limiter |
| **Database** | Android Jetpack Room with Kotlin Symbol Processing (KSP) |
| **File Storage** | Android Scoped Storage (`MediaStore.Downloads`) & POSIX File I/O with MediaScanner |
| **CI / CD** | GitHub Actions Automated APK Builder & Release Pipeline (`.github/workflows/build-apk.yml`) |

---

## 🚀 Building from Source

### Prerequisites
- Android Studio Ladybug (2024.2+) or newer
- JDK 17 (Eclipse Temurin or OpenJDK)
- Android SDK Platform 36

### Build Commands

```bash
# Clone the repository
git clone https://github.com/tapos-linx/LR_Mass_Downloader.git
cd LR_Mass_Downloader

# Grant execution rights to the Gradle wrapper
chmod +x gradlew

# Build Debug APK
./gradlew assembleDebug

# Output APK path:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 License & Credits

- Developed for Bangladesh Land Records & Survey (ভূমি রেকর্ড ও জরিপ অধিদপ্তর) automation.
- Licensed under the [MIT License](LICENSE).
