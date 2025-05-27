package io.novafoundation.nova.app.root.di.deeplink

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.di.deeplinks.AccountDeepLinks
import io.novafoundation.nova.feature_assets.di.modules.deeplinks.AssetDeepLinks
import io.novafoundation.nova.feature_buy_api.di.deeplinks.BuyDeepLinks
import io.novafoundation.nova.feature_dapp_api.di.deeplinks.DAppDeepLinks
import io.novafoundation.nova.feature_deep_linking.presentation.handling.CompoundDeepLinkHandler
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkHandler
import io.novafoundation.nova.feature_deep_linking.presentation.handling.RootDeepLinkHandler
import io.novafoundation.nova.feature_deep_linking.presentation.handling.branchIo.BranchIOLinkHandler
import io.novafoundation.nova.feature_deep_linking.presentation.handling.branchIo.BranchIoLinkConverter
import io.novafoundation.nova.feature_governance_api.di.deeplinks.GovernanceDeepLinks
import io.novafoundation.nova.feature_staking_api.di.deeplinks.StakingDeepLinks
import io.novafoundation.nova.feature_wallet_connect_api.di.deeplinks.WalletConnectDeepLinks

@Module
class DeepLinksModule {

    @Provides
    @FeatureScope
    fun provideDeepLinkHandlers(
        stakingDeepLinks: StakingDeepLinks,
        accountDeepLinks: AccountDeepLinks,
        dAppDeepLinks: DAppDeepLinks,
        governanceDeepLinks: GovernanceDeepLinks,
        buyDeepLinks: BuyDeepLinks,
        assetDeepLinks: AssetDeepLinks,
        walletConnectDeepLinks: WalletConnectDeepLinks
    ): List<@JvmWildcard DeepLinkHandler> {
        return stakingDeepLinks.deepLinkHandlers.asSequence()
            .plus(accountDeepLinks.deepLinkHandlers)
            .plus(dAppDeepLinks.deepLinkHandlers)
            .plus(governanceDeepLinks.deepLinkHandlers)
            .plus(buyDeepLinks.deepLinkHandlers)
            .plus(assetDeepLinks.deepLinkHandlers)
            .plus(walletConnectDeepLinks.deepLinkHandlers)
            .toList()
    }

    @Provides
    @FeatureScope
    fun provideCompoundDeepLinkHandler(
        nestedHandlers: @JvmWildcard List<DeepLinkHandler>
    ): CompoundDeepLinkHandler {

        return CompoundDeepLinkHandler(nestedHandlers)
    }

    @Provides
    @FeatureScope
    fun provideRootDeepLinkHandler(
        baseHandler: CompoundDeepLinkHandler
    ): RootDeepLinkHandler {

        return RootDeepLinkHandler(baseHandler)
    }

    @Provides
    @FeatureScope
    fun provideExternalDeepLinkHandler(
        branchIoLinkConverter: BranchIoLinkConverter
    ): BranchIOLinkHandler {
        return BranchIOLinkHandler(branchIoLinkConverter)
    }
}
