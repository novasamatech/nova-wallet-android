package io.novafoundation.nova.feature_account_impl.presentation.seedScan

import io.novafoundation.nova.common.presentation.scan.ScanQrViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.domain.scanSeed.ScanSeedInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter

class ScanSeedViewModel(
    private val router: AccountRouter,
    permissionsAsker: PermissionsAsker.Presentation,
    private val interactor: ScanSeedInteractor,
    private val resourceManager: ResourceManager,
    private val scanSeedResponder: ScanSeedResponder
) : ScanQrViewModel(permissionsAsker) {

    val title = resourceManager.getString(R.string.common_scan_qr_code)

    fun backClicked() {
        router.back()
    }

    override suspend fun scanned(result: String) {
        val parseResult = interactor.decodeSeed(result)

        parseResult
            .onSuccess(::openPreview)
            .onFailure {
                val message = resourceManager.getString(R.string.common_invalid_qr_code)
                showToast(message)

                resetScanningThrottled()
            }
    }

    private fun openPreview(seed: String) {
        scanSeedResponder.respond(ScanSeedCommunicator.Response(seed))
        router.back()
    }
}
