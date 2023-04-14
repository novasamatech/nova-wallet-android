package io.novafoundation.nova.feature_dapp_impl.walletConnect.presentation.sessions

import android.util.Log
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Web3Wallet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn

sealed class WalletConnectSessionsEvent {

    data class SessionProposal(val proposal: Wallet.Model.SessionProposal) : WalletConnectSessionsEvent()

    data class SessionRequest(val request: Wallet.Model.SessionRequest) : WalletConnectSessionsEvent()
}

fun Web3Wallet.sessionEventsFlow(scope: CoroutineScope): Flow<WalletConnectSessionsEvent> {
    return callbackFlow {
        setWalletDelegate(object : Web3Wallet.WalletDelegate {

            override fun onAuthRequest(authRequest: Wallet.Model.AuthRequest) {
            }

            override fun onConnectionStateChange(state: Wallet.Model.ConnectionState) {
            }

            override fun onError(error: Wallet.Model.Error) {
                Log.e("WalletConnect", "Wallet Connect error", error.throwable)
            }

            override fun onSessionDelete(sessionDelete: Wallet.Model.SessionDelete) {
            }

            override fun onSessionProposal(sessionProposal: Wallet.Model.SessionProposal) {
                channel.trySend(WalletConnectSessionsEvent.SessionProposal(sessionProposal))
            }

            override fun onSessionRequest(sessionRequest: Wallet.Model.SessionRequest) {
                channel.trySend(WalletConnectSessionsEvent.SessionRequest(sessionRequest))
            }

            override fun onSessionSettleResponse(settleSessionResponse: Wallet.Model.SettledSessionResponse) {
            }

            override fun onSessionUpdateResponse(sessionUpdateResponse: Wallet.Model.SessionUpdateResponse) {
            }
        })

        awaitClose { }
    }.shareIn(scope, SharingStarted.Eagerly)
}
