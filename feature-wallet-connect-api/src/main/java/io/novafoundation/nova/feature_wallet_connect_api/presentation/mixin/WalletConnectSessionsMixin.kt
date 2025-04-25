package io.novafoundation.nova.feature_wallet_connect_api.presentation.mixin

import io.novafoundation.nova.feature_wallet_connect_api.presentation.WalletConnectSessionsModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface WalletConnectSessionsMixinFactory {
    fun create(coroutineScope: CoroutineScope): WalletConnectSessionsMixin
}

interface WalletConnectSessionsMixin {
    fun getActiveSessionsForSelectedAccount(): Flow<WalletConnectSessionsModel>

    fun onWalletConnectClick()
}
