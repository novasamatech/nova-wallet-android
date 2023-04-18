package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions

import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Web3Wallet
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_wallet_connect_impl.WalletConnectRouter
import io.novafoundation.nova.feature_wallet_connect_impl.WalletConnectScanCommunicator
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class WalletConnectSessionsViewModel(
    private val router: WalletConnectRouter,
    private val scanCommunicator: WalletConnectScanCommunicator,
) : BaseViewModel() {

    init {
        scanCommunicator.responseFlow.onEach {
            Web3Wallet.pair(Wallet.Params.Pair(it.wcUri), onError = { showError(it.throwable) })
        }
            .launchIn(this)
    }

    fun exit() {
        router.back()
    }

    fun initiateScan() {
        scanCommunicator.openRequest(WalletConnectScanCommunicator.Request())
    }
}
