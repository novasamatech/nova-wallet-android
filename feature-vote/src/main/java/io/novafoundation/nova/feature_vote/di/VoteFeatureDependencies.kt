package io.novafoundation.nova.feature_vote.di

import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_wallet_connect_api.presentation.mixin.WalletConnectSessionsMixinFactory

interface VoteFeatureDependencies {

    val selectedAccountUseCase: SelectedAccountUseCase

    val walletConnectSessionsMixinFactory: WalletConnectSessionsMixinFactory
}
