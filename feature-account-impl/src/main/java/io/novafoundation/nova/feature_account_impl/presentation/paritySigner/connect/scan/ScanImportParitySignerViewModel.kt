package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.scan

import android.Manifest
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.common.utils.sendEvent
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.paritySigner.connect.scan.ParitySignerAccount
import io.novafoundation.nova.feature_account_impl.domain.paritySigner.connect.scan.ScanImportParitySignerInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.ParitySignerAccountPayload
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ScanImportParitySignerViewModel(
    private val router: AccountRouter,
    private val permissionsAsker: PermissionsAsker.Presentation,
    private val interactor: ScanImportParitySignerInteractor,
    private val resourceManager: ResourceManager
) : BaseViewModel(), PermissionsAsker by permissionsAsker {

    val scanningAvailable = MutableStateFlow(false)

    private val _resetScanningEvent = MutableLiveData<Event<Unit>>()
    val resetScanningEvent: LiveData<Event<Unit>> = _resetScanningEvent

    fun backClicked() {
        router.back()
    }

    fun onStart() {
        requirePermissions()
    }

    fun scanned(result: String) = launch {
        val parseResult = interactor.decodeScanResult(result)

        parseResult
            .onSuccess(::openPreview)
            .onFailure {
                showMessage(resourceManager.getString(R.string.account_parity_signer_import_scan_invalid_qr))

                // wait a bit until re-enabling scanner otherwise user might experience a lot of error messages shown due to fast scanning
                delay(1000)

                _resetScanningEvent.sendEvent()
            }
    }

    private fun openPreview(signerAccount: ParitySignerAccount) {
        val payload = ParitySignerAccountPayload(signerAccount.accountId)

        router.openPreviewImportParitySigner(payload)
    }

    private fun requirePermissions() = launch {
        val granted = permissionsAsker.requirePermissionsOrExit(Manifest.permission.CAMERA)

        scanningAvailable.value = granted
    }
}
