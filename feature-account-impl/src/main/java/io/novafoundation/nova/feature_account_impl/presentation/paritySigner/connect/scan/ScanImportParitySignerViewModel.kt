package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.scan

import io.novafoundation.nova.common.presentation.scan.ScanQrViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.paritySigner.connect.scan.ParitySignerAccount
import io.novafoundation.nova.feature_account_impl.domain.paritySigner.connect.scan.ScanImportParitySignerInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.connect.ParitySignerAccountPayload
import kotlinx.coroutines.delay

class ScanImportParitySignerViewModel(
    private val router: AccountRouter,
    permissionsAsker: PermissionsAsker.Presentation,
    private val interactor: ScanImportParitySignerInteractor,
    private val resourceManager: ResourceManager
) : ScanQrViewModel(permissionsAsker) {

    fun backClicked() {
        router.back()
    }

    override suspend fun scanned(result: String) {
        val parseResult = interactor.decodeScanResult(result)

        parseResult
            .onSuccess(::openPreview)
            .onFailure {
                showMessage(resourceManager.getString(R.string.account_parity_signer_import_scan_invalid_qr))

                // wait a bit until re-enabling scanner otherwise user might experience a lot of error messages shown due to fast scanning
                delay(1000)

                resetScanning()
            }
    }

    private fun openPreview(signerAccount: ParitySignerAccount) {
        val payload = ParitySignerAccountPayload(signerAccount.accountId)

        router.openPreviewImportParitySigner(payload)
    }
}
