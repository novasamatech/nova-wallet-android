package io.novafoundation.nova.app.root.di

import io.novafoundation.nova.app.root.navigation.holders.RootNavigationHolder
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.Navigator
import io.novafoundation.nova.app.root.navigation.navigators.staking.StakingDashboardNavigator
import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.common.navigation.DelayedNavigationRouter
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_migration.di.AccountMigrationFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_buy_api.di.BuyFeatureApi
import io.novafoundation.nova.feature_crowdloan_api.di.CrowdloanFeatureApi
import io.novafoundation.nova.feature_currency_api.di.CurrencyFeatureApi
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_deep_linking.di.DeepLinkingFeatureApi
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_ledger_api.di.LedgerFeatureApi
import io.novafoundation.nova.feature_multisig_operations.di.MultisigOperationsFeatureApi
import io.novafoundation.nova.feature_push_notifications.di.PushNotificationsFeatureApi
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_versions_api.di.VersionsFeatureApi
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.feature_wallet_connect_api.di.WalletConnectFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi
import javax.inject.Inject

@ApplicationScope
class RootFeatureHolder @Inject constructor(
    private val splitScreenNavigationHolder: SplitScreenNavigationHolder,
    private val rootNavigationHolder: RootNavigationHolder,
    private val navigator: Navigator,
    private val governanceRouter: GovernanceRouter,
    private val dAppRouter: DAppRouter,
    private val accountRouter: AccountRouter,
    private val assetsRouter: AssetsRouter,
    private val stakingRouter: StakingRouter,
    private val stakingDashboardNavigator: StakingDashboardNavigator,
    private val delayedNavRouter: DelayedNavigationRouter,
    featureContainer: FeatureContainer
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val rootFeatureDependencies = DaggerRootComponent_RootFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .dbApi(getFeature(DbApi::class.java))
            .accountFeatureApi(getFeature(AccountFeatureApi::class.java))
            .walletFeatureApi(getFeature(WalletFeatureApi::class.java))
            .stakingFeatureApi(getFeature(StakingFeatureApi::class.java))
            .assetsFeatureApi(getFeature(AssetsFeatureApi::class.java))
            .currencyFeatureApi(getFeature(CurrencyFeatureApi::class.java))
            .crowdloanFeatureApi(getFeature(CrowdloanFeatureApi::class.java))
            .governanceFeatureApi(getFeature(GovernanceFeatureApi::class.java))
            .dAppFeatureApi(getFeature(DAppFeatureApi::class.java))
            .runtimeApi(getFeature(RuntimeApi::class.java))
            .versionsFeatureApi(getFeature(VersionsFeatureApi::class.java))
            .walletConnectFeatureApi(getFeature(WalletConnectFeatureApi::class.java))
            .pushNotificationsFeatureApi(getFeature(PushNotificationsFeatureApi::class.java))
            .deepLinkingFeatureApi(getFeature(DeepLinkingFeatureApi::class.java))
            .ledgerFeatureApi(getFeature(LedgerFeatureApi::class.java))
            .buyFeatureApi(getFeature(BuyFeatureApi::class.java))
            .deepLinkingFeatureApi(getFeature(DeepLinkingFeatureApi::class.java))
            .accountMigrationFeatureApi(getFeature(AccountMigrationFeatureApi::class.java))
            .multisigOperationsFeatureApi(getFeature(MultisigOperationsFeatureApi::class.java))
            .build()

        return DaggerRootComponent.factory()
            .create(
                splitScreenNavigationHolder,
                rootNavigationHolder,
                navigator,
                governanceRouter,
                dAppRouter,
                assetsRouter,
                accountRouter,
                stakingRouter,
                stakingDashboardNavigator,
                delayedNavRouter,
                rootFeatureDependencies
            )
    }
}
