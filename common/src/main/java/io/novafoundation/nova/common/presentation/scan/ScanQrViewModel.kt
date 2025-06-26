package io.novafoundation.nova.common.presentation.scan

import android.Manifest
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.common.utils.sendEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

abstract class ScanQrViewModel(
    private val permissionsAsker: PermissionsAsker.Presentation,
) : BaseViewModel(), PermissionsAsker by permissionsAsker {

    val scanningAvailable = MutableStateFlow(false)

    private val _resetScanningEvent = MutableLiveData<Event<Unit>>()
    val resetScanningEvent: LiveData<Event<Unit>> = _resetScanningEvent

    protected abstract suspend fun scanned(result: String)

    fun onScanned(result: String) {
        launch {
            scanned(result)
        }
    }

    fun onStart() {
        requirePermissions()
    }

    protected fun resetScanning() {
        _resetScanningEvent.sendEvent()
    }

    private fun requirePermissions() = launch {
        val granted = permissionsAsker.requirePermissions(Manifest.permission.CAMERA)

        scanningAvailable.value = granted
    }
}
