package io.novafoundation.nova.feature_wallet_connect_api.di

import io.novafoundation.nova.feature_wallet_connect_api.di.deeplinks.WalletConnectDeepLinks
import io.novafoundation.nova.feature_wallet_connect_api.domain.sessions.WalletConnectSessionsUseCase
import io.novafoundation.nova.feature_wallet_connect_api.presentation.WalletConnectService

interface WalletConnectFeatureApi {

    val walletConnectService: WalletConnectService

    val sessionsUseCase: WalletConnectSessionsUseCase

    val walletConnectDeepLinks: WalletConnectDeepLinks
}
