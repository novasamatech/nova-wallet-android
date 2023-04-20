package io.novafoundation.nova.feature_wallet_connect_api.di

import io.novafoundation.nova.feature_wallet_connect_api.presentation.WalletConnectService

interface WalletConnectFeatureApi {

    val walletConnectServiceFactory: WalletConnectService.Factory
}
