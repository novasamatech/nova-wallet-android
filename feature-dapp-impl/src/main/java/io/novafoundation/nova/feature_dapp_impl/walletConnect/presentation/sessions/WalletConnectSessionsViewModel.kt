package io.novafoundation.nova.feature_dapp_impl.walletConnect.presentation.sessions

import android.util.Log
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.web3.wallet.client.Web3Wallet
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.walletConnect.WalletConnectScanCommunicator
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class WalletConnectSessionsViewModel(
    private val router: DAppRouter,
    private val scanCommunicator: WalletConnectScanCommunicator,
): BaseViewModel() {

    init {
        CoreClient.Relay.connect { _: Core.Model.Error -> }
    }

    val events = Web3Wallet.sessionEventsFlow(scope = this)

    init {
        events.onEach {
            Log.d(LOG_TAG, "Received event: $it")
        }.launchIn(this)

        scanCommunicator.responseFlow.onEach {
            showMessage(it.wcUri)
        }
            .launchIn(this)
    }

    fun exit() {
        router.back()
    }

    override fun onCleared() {
        super.onCleared()

        CoreClient.Relay.disconnect {  _: Core.Model.Error ->  }
    }

    fun initiateScan() {
        scanCommunicator.openRequest(WalletConnectScanCommunicator.Request())
    }
}
