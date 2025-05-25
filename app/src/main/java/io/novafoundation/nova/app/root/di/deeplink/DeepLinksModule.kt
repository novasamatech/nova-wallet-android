package io.novafoundation.nova.app.root.di.deeplink

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.di.deeplinks.AccountDeepLinks
import io.novafoundation.nova.feature_assets.di.modules.deeplinks.AssetDeepLinks
import io.novafoundation.nova.feature_buy_api.di.deeplinks.BuyDeepLinks
import io.novafoundation.nova.feature_dapp_api.di.deeplinks.DAppDeepLinks
import io.novafoundation.nova.feature_deep_linking.presentation.handling.RootDeepLinkHandler
import io.novafoundation.nova.feature_governance_api.di.deeplinks.GovernanceDeepLinks
import io.novafoundation.nova.feature_staking_api.di.deeplinks.StakingDeepLinks
import io.novafoundation.nova.feature_wallet_connect_api.di.deeplinks.WalletConnectDeepLinks

@Module
class DeepLinksModule {

    @Provides
    @FeatureScope
    fun provideRootDeepLinkHandler(
        stakingDeepLinks: StakingDeepLinks,
        accountDeepLinks: AccountDeepLinks,
        dAppDeepLinks: DAppDeepLinks,
        governanceDeepLinks: GovernanceDeepLinks,
        buyDeepLinks: BuyDeepLinks,
        assetDeepLinks: AssetDeepLinks,
        walletConnectDeepLinks: WalletConnectDeepLinks
    ): RootDeepLinkHandler {
        val deepLinkHandlers = stakingDeepLinks.deepLinkHandlers.asSequence()
            .plus(accountDeepLinks.deepLinkHandlers)
            .plus(dAppDeepLinks.deepLinkHandlers)
            .plus(governanceDeepLinks.deepLinkHandlers)
            .plus(buyDeepLinks.deepLinkHandlers)
            .plus(assetDeepLinks.deepLinkHandlers)
            .plus(walletConnectDeepLinks.deepLinkHandlers)
            .toList()

        return RootDeepLinkHandler(deepLinkHandlers)
    }
}
