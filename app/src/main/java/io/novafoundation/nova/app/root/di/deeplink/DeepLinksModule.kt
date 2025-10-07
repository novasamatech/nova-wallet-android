package io.novafoundation.nova.app.root.di.deeplink

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.di.deeplinks.AccountDeepLinks
import io.novafoundation.nova.feature_account_migration.di.deeplinks.AccountMigrationDeepLinks
import io.novafoundation.nova.feature_ahm_api.di.deeplinks.ChainMigrationDeepLinks
import io.novafoundation.nova.feature_assets.di.modules.deeplinks.AssetDeepLinks
import io.novafoundation.nova.feature_buy_api.di.deeplinks.BuyDeepLinks
import io.novafoundation.nova.feature_dapp_api.di.deeplinks.DAppDeepLinks
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkHandler
import io.novafoundation.nova.feature_deep_linking.presentation.handling.PendingDeepLinkProvider
import io.novafoundation.nova.feature_deep_linking.presentation.handling.RootDeepLinkHandler
import io.novafoundation.nova.feature_deep_linking.presentation.handling.branchIo.BranchIOLinkHandler
import io.novafoundation.nova.feature_deep_linking.presentation.handling.branchIo.BranchIoLinkConverter
import io.novafoundation.nova.feature_governance_api.di.deeplinks.GovernanceDeepLinks
import io.novafoundation.nova.feature_multisig_operations.di.deeplink.MultisigDeepLinks
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
        walletConnectDeepLinks: WalletConnectDeepLinks,
        accountMigrationDeepLinks: AccountMigrationDeepLinks,
        multisigDeepLinks: MultisigDeepLinks,
        chainMigrationDeepLinks: ChainMigrationDeepLinks
    ): List<@JvmWildcard DeepLinkHandler> {
        return buildList {
            addAll(stakingDeepLinks.deepLinkHandlers)
            addAll(accountDeepLinks.deepLinkHandlers)
            addAll(dAppDeepLinks.deepLinkHandlers)
            addAll(governanceDeepLinks.deepLinkHandlers)
            addAll(buyDeepLinks.deepLinkHandlers)
            addAll(assetDeepLinks.deepLinkHandlers)
            addAll(walletConnectDeepLinks.deepLinkHandlers)
            addAll(accountMigrationDeepLinks.deepLinkHandlers)
            addAll(multisigDeepLinks.deepLinkHandlers)
            addAll(chainMigrationDeepLinks.deepLinkHandlers)
        }
    }

    @Provides
    @FeatureScope
    fun provideRootDeepLinkHandler(
        pendingDeepLinkProvider: PendingDeepLinkProvider,
        nestedHandlers: @JvmWildcard List<DeepLinkHandler>
    ): RootDeepLinkHandler {
        return RootDeepLinkHandler(
            pendingDeepLinkProvider,
            nestedHandlers
        )
    }

    @Provides
    @FeatureScope
    fun provideBranchIOLinkHandler(
        branchIoLinkConverter: BranchIoLinkConverter
    ): BranchIOLinkHandler {
        return BranchIOLinkHandler(branchIoLinkConverter)
    }
}
