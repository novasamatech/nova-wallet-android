package io.novafoundation.nova.feature_dapp_impl.walletConnect.presentation.scan

import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.common.presentation.scan.ScanQrViewModel
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.feature_dapp_impl.walletConnect.WalletConnectScanCommunicator

class WalletConnectScanViewModel(
    private val router: ReturnableRouter,
    private val permissionsAsker: PermissionsAsker.Presentation,
    private val communicator: WalletConnectScanCommunicator
) : ScanQrViewModel(permissionsAsker) {

    fun backClicked() {
        router.back()
    }

    override suspend fun scanned(result: String) {
        communicator.respond(WalletConnectScanCommunicator.Response(wcUri = result))
        router.back()
    }
}
