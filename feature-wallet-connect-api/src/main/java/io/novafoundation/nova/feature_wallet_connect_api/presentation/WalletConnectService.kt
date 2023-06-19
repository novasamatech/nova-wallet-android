package io.novafoundation.nova.feature_wallet_connect_api.presentation

interface WalletConnectService {

    fun connect()

    fun disconnect()

    fun pair(uri: String)

    fun setOnPairErrorCallback(callback: (throwable: Throwable) -> Unit)
}
