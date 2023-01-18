package io.novafoundation.nova.common.sequrity

import android.view.WindowManager
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.resources.ContextManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface SafeModeService {
    fun isSafeModeEnabled(): Boolean

    fun safeModeStatusFlow(): Flow<Boolean>

    fun applySafeModeIfEnabled()

    fun setSafeMode(enable: Boolean)

    fun toggleSafeMode()
}

class RealSafeModeService(
    private val contextManager: ContextManager,
    private val preferences: Preferences
) : SafeModeService {

    companion object {
        private const val PREF_SAFE_MODE_STATUS = "safe_mode_status"
    }

    private val safeModeStatus = MutableStateFlow(getSafeModeStatus())

    override fun isSafeModeEnabled(): Boolean {
        return safeModeStatus.value
    }

    override fun safeModeStatusFlow(): Flow<Boolean> {
        return safeModeStatus
    }

    override fun applySafeModeIfEnabled() {
        if (safeModeStatus.value) {
            contextManager.getActivity()?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun setSafeMode(enable: Boolean) {
        preferences.putBoolean(PREF_SAFE_MODE_STATUS, enable)

        if (enable) {
            contextManager.getActivity()?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            contextManager.getActivity()?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        safeModeStatus.value = enable
    }

    override fun toggleSafeMode() {
        setSafeMode(!safeModeStatus.value)
    }

    private fun getSafeModeStatus(): Boolean {
        return preferences.getBoolean(PREF_SAFE_MODE_STATUS, false)
    }
}
