package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.list

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

    data class SessionSettlement(val settlement: Wallet.Model.SettledSessionResponse) : WalletConnectSessionsEvent()

    data class SessionDeleted(val delete: Wallet.Model.SessionDelete) : WalletConnectSessionsEvent()
}

fun Web3Wallet.sessionEventsFlow(scope: CoroutineScope): Flow<WalletConnectSessionsEvent> {
    return callbackFlow {
        setWalletDelegate(object : Web3Wallet.WalletDelegate {

            override fun onAuthRequest(authRequest: Wallet.Model.AuthRequest, verifyContext: Wallet.Model.VerifyContext) {
                Log.d("WalletConnect", "Auth request: $authRequest")
            }

            override fun onConnectionStateChange(state: Wallet.Model.ConnectionState) {
                Log.d("WalletConnect", "on connection state change: $state")
            }

            override fun onError(error: Wallet.Model.Error) {
                Log.e("WalletConnect", "Wallet Connect error", error.throwable)
            }

            override fun onProposalExpired(proposal: Wallet.Model.ExpiredProposal) {
                Log.d("WalletConnect", "Proposal expired: $proposal")
            }

            override fun onRequestExpired(request: Wallet.Model.ExpiredRequest) {
                Log.d("WalletConnect", "Request expired: $request")
            }

            override fun onSessionDelete(sessionDelete: Wallet.Model.SessionDelete) {
                Log.d("WalletConnect", "on session delete: $sessionDelete")
                channel.trySend(WalletConnectSessionsEvent.SessionDeleted(sessionDelete))
            }

            override fun onSessionExtend(session: Wallet.Model.Session) {
                Log.d("WalletConnect", "On session extend: $session")
            }

            override fun onSessionProposal(sessionProposal: Wallet.Model.SessionProposal, verifyContext: Wallet.Model.VerifyContext) {
                Log.d("WalletConnect", "on session proposal: $sessionProposal")
                channel.trySend(WalletConnectSessionsEvent.SessionProposal(sessionProposal))
            }

            override fun onSessionRequest(sessionRequest: Wallet.Model.SessionRequest, verifyContext: Wallet.Model.VerifyContext) {
                Log.d("WalletConnect", "on session request: $sessionRequest")
                channel.trySend(WalletConnectSessionsEvent.SessionRequest(sessionRequest))
            }

            override fun onSessionSettleResponse(settleSessionResponse: Wallet.Model.SettledSessionResponse) {
                Log.d("WalletConnect", "on session settled: $settleSessionResponse")
                channel.trySend(WalletConnectSessionsEvent.SessionSettlement(settleSessionResponse))
            }

            override fun onSessionUpdateResponse(sessionUpdateResponse: Wallet.Model.SessionUpdateResponse) {
                Log.d("WalletConnect", "on session update: $sessionUpdateResponse")
            }
        })

        awaitClose { }
    }.shareIn(scope, SharingStarted.Eagerly)
}
