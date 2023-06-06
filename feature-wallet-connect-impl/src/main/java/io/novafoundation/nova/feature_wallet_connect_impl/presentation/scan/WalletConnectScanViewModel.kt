package io.novafoundation.nova.feature_wallet_connect_impl.presentation.scan

import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Web3Wallet
import io.novafoundation.nova.common.navigation.ReturnableRouter
import io.novafoundation.nova.common.presentation.scan.ScanQrViewModel
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker

class WalletConnectScanViewModel(
    private val router: ReturnableRouter,
    private val permissionsAsker: PermissionsAsker.Presentation,
) : ScanQrViewModel(permissionsAsker) {

    fun backClicked() {
        router.back()
    }

    override suspend fun scanned(result: String) {
        initiatePairing(result)

        router.back()
    }

    private fun initiatePairing(uri: String) {
        Web3Wallet.pair(Wallet.Params.Pair(uri), onError = { showError(it.throwable) })
    }
}
