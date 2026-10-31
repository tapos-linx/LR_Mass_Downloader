package com.example.util

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

enum class NetworkTier(val labelEn: String, val labelBn: String) {
    HIGH_SPEED_WIFI("High-Speed Wi-Fi / Ethernet", "হাই-স্পিড ওয়াই-ফাই"),
    FAST_CELLULAR("4G LTE / 5G Mobile Network", "৪জি/৫জি মোবাইল নেটওয়ার্ক"),
    CONSTRAINED_NETWORK("3G / Metered / Constrained", "সীমিত বা ধীরগতির নেটওয়ার্ক"),
    OFFLINE_LOCAL("Local / Standby Network", "অফলাইন / লোকাল মোড")
}

enum class DeviceTier(val labelEn: String, val labelBn: String) {
    HIGH_PERFORMANCE("High-End Multi-Core", "উচ্চ ক্ষমতাসম্পন্ন ডিভাইস"),
    BALANCED("Balanced Mid-Range", "ভারসাম্যপূর্ণ ডিভাইস"),
    LOW_MEMORY("Low-RAM / Power Saver", "কম র‍্যাম / পাওয়ার সেভার")
}

data class DeviceNetworkDependencyProfile(
    val networkTier: NetworkTier,
    val deviceTier: DeviceTier,
    val cpuCores: Int,
    val totalRamMb: Long,
    val availableRamMb: Long,
    val downstreamBandwidthKbps: Int,
    val isMetered: Boolean,
    val mouzasPerQueue: Int,
    val maxConcurrentDownloads: Int,
    val stepDelayMs: Long
) {
    val profileTitleEn: String
        get() = "${networkTier.labelEn} • ${deviceTier.labelEn}"

    val profileTitleBn: String
        get() = "${networkTier.labelBn} • ${deviceTier.labelBn} ($cpuCores কোর, ${totalRamMb}MB RAM)"

    val queueRuleDescriptionBn: String
        get() = "নেটওয়ার্ক ও ডিভাইসের সক্ষমতা অনুযায়ী প্রতি কিউতে $mouzasPerQueue টি মৌজা (সমান্তরাল $maxConcurrentDownloads টি স্ট্রিম) নির্ধারণ করা হয়েছে। একটি কিউ শেষ হলে স্বয়ংক্রিয়ভাবে পরবর্তী কিউ শুরু হবে।"

    val queueRuleDescriptionEn: String
        get() = "Adaptive $mouzasPerQueue Mouzas/Queue ($maxConcurrentDownloads parallel streams) for $cpuCores-core / ${totalRamMb}MB RAM on ${networkTier.labelEn}. Queues execute sequentially 1 by 1 until all Mouzas complete."
}

object NetworkDeviceQueueAdvisor {

    fun analyze(context: Context): DeviceNetworkDependencyProfile {
        val cpuCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)

        var totalRamMb = 4096L
        var availableRamMb = 2048L
        var isLowMemoryDevice = false

        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            if (activityManager != null) {
                val memInfo = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(memInfo)
                if (memInfo.totalMem > 0) {
                    totalRamMb = memInfo.totalMem / (1024L * 1024L)
                }
                if (memInfo.availMem > 0) {
                    availableRamMb = memInfo.availMem / (1024L * 1024L)
                }
                isLowMemoryDevice = memInfo.lowMemory || activityManager.isLowRamDevice
            }
        } catch (ignored: Exception) {
        }

        val deviceTier = when {
            isLowMemoryDevice || totalRamMb < 2800L || cpuCores <= 2 -> DeviceTier.LOW_MEMORY
            cpuCores >= 8 && totalRamMb >= 5500L -> DeviceTier.HIGH_PERFORMANCE
            else -> DeviceTier.BALANCED
        }

        var networkTier = NetworkTier.HIGH_SPEED_WIFI
        var downstreamKbps = 25000
        var isMetered = false

        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = cm?.activeNetwork
            val caps = if (activeNetwork != null) cm.getNetworkCapabilities(activeNetwork) else null

            if (caps != null) {
                isMetered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                val bw = caps.linkDownstreamBandwidthKbps
                if (bw > 0) downstreamKbps = bw

                networkTier = when {
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                        if (downstreamKbps in 1..4000) NetworkTier.CONSTRAINED_NETWORK
                        else NetworkTier.HIGH_SPEED_WIFI
                    }
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        if (downstreamKbps >= 8000) NetworkTier.FAST_CELLULAR
                        else NetworkTier.CONSTRAINED_NETWORK
                    }
                    else -> NetworkTier.FAST_CELLULAR
                }
            }
        } catch (ignored: Exception) {
        }

        // Standard 7-Mouza batching per Section 5 specification (Batch 1: 1-7, Batch 2: 8-14, ...)
        // while adapting concurrent worker streams and anti-blocking jitter to network & device hardware.
        val mouzasPerQueue = 7

        val maxConcurrentDownloads = when {
            deviceTier == DeviceTier.LOW_MEMORY || networkTier == NetworkTier.CONSTRAINED_NETWORK -> 1
            deviceTier == DeviceTier.HIGH_PERFORMANCE && networkTier == NetworkTier.HIGH_SPEED_WIFI -> 3
            else -> 2
        }

        val stepDelayMs = when (networkTier) {
            NetworkTier.HIGH_SPEED_WIFI -> 75L
            NetworkTier.FAST_CELLULAR -> 95L
            NetworkTier.CONSTRAINED_NETWORK -> 130L
            NetworkTier.OFFLINE_LOCAL -> 90L
        }

        return DeviceNetworkDependencyProfile(
            networkTier = networkTier,
            deviceTier = deviceTier,
            cpuCores = cpuCores,
            totalRamMb = totalRamMb,
            availableRamMb = availableRamMb,
            downstreamBandwidthKbps = downstreamKbps,
            isMetered = isMetered,
            mouzasPerQueue = mouzasPerQueue,
            maxConcurrentDownloads = maxConcurrentDownloads,
            stepDelayMs = stepDelayMs
        )
    }
}
