package io.novafoundation.nova.feature_wallet_connect_impl.presentation.scan

import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.common.presentation.scan.ScanQrViewModel
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.feature_wallet_connect_api.presentation.WalletConnectService

class WalletConnectScanViewModel(
    private val router: ReturnableRouter,
    private val permissionsAsker: PermissionsAsker.Presentation,
    private val walletConnectService: WalletConnectService
) : ScanQrViewModel(permissionsAsker) {

    fun backClicked() {
        router.back()
    }

    override suspend fun scanned(result: String) {
        initiatePairing(result)

        router.back()
    }

    private fun initiatePairing(uri: String) {
        walletConnectService.pair(uri)
    }
}
