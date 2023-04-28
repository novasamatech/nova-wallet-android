package io.novafoundation.nova.feature_wallet_connect_api.presentation

import kotlinx.coroutines.CoroutineScope

interface WalletConnectService {

    interface Factory {

        fun create(coroutineScope: CoroutineScope): WalletConnectService
    }

    fun pair(wcUri: String)

    fun connect()

    fun disconnect()
}
