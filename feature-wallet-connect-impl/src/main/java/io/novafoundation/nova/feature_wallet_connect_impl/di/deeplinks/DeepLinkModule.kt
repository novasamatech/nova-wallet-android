package io.novafoundation.nova.feature_wallet_connect_impl.di.deeplinks

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.feature_wallet_connect_api.di.deeplinks.WalletConnectDeepLinks
import io.novafoundation.nova.feature_wallet_connect_api.presentation.WalletConnectService
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.deeplink.WalletConnectPairDeeplinkHandler

@Module
class DeepLinkModule {

    @Provides
    @FeatureScope
    fun provideWalletConnectDeepLinkHandler(
        walletConnectService: WalletConnectService,
        automaticInteractionGate: AutomaticInteractionGate
    ) = WalletConnectPairDeeplinkHandler(
        walletConnectService,
        automaticInteractionGate
    )
    @Provides
    @FeatureScope
    fun provideDeepLinks(buyCallback: WalletConnectPairDeeplinkHandler): WalletConnectDeepLinks {
        return WalletConnectDeepLinks(listOf(buyCallback))
    }
}
