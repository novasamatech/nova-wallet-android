package io.novafoundation.nova.feature_dapp_impl.utils.tabs

import android.app.ActivityManager
import android.content.Context
import io.novafoundation.nova.common.utils.InformationSizeUnit
import io.novafoundation.nova.common.utils.toInformationSize

private const val MIN_TABS = 3
private val MEMORY_STEP = 100.toInformationSize(InformationSizeUnit.MEGABYTES).sizeInBytes

interface TabMemoryRestrictionService {
    fun getMaximumActiveSessions(): Int
}

class RealTabMemoryRestrictionService(val context: Context) : TabMemoryRestrictionService {

    // The linear function that starts from 3 and adds 1 tab each MEMORY_STEP of available memory
    override fun getMaximumActiveSessions(): Int {
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
