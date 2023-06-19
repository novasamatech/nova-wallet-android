package io.novafoundation.nova.feature_wallet_connect_api.presentation

import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.utils.Event

interface WalletConnectService {

    val onPairErrorLiveData: LiveData<Event<Throwable>>

    fun connect()

    fun disconnect()

    fun pair(uri: String)
}
