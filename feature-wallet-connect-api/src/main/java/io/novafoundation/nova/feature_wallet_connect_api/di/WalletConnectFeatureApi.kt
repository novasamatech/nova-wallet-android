package io.novafoundation.nova.feature_wallet_connect_api.di

import io.novafoundation.nova.feature_wallet_connect_api.domain.sessions.WalletConnectSessionsUseCase
import io.novafoundation.nova.feature_wallet_connect_api.presentation.WalletConnectService
import io.novafoundation.nova.feature_wallet_connect_api.presentation.mixin.WalletConnectSessionsMixinFactory

interface WalletConnectFeatureApi {

    val walletConnectService: WalletConnectService

    val sessionsUseCase: WalletConnectSessionsUseCase

    val walletConnectSessionsMixinFactory: WalletConnectSessionsMixinFactory
}
