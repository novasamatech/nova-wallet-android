package io.novafoundation.nova.feature_dapp_impl.utils.tabs

import android.app.ActivityManager
import android.content.Context

private const val MIN_TABS = 3
private const val MEMORY_STEP = 100L * 1024L * 1024L // 100 Mb

class TabMemoryRestrictionService(val context: Context) {

    // The linear function that starts from 3 and adds 1 tab each MEMORY_STEP of available memory
    fun getMaximumActiveSessions(): Int {
        val availableMemory = getAvailableMemory()
        return MIN_TABS + (availableMemory / MEMORY_STEP).toInt()
    }

    private fun getAvailableMemory(): Long {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.availMem
    }
}
