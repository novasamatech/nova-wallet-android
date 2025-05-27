package io.novafoundation.nova.app.root.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.app.root.navigation.holders.RootNavigationHolder
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
import io.novafoundation.nova.app.root.navigation.navigators.staking.StakingDashboardNavigator
import io.novafoundation.nova.app.root.presentation.RootRouter
import io.novafoundation.nova.app.root.presentation.di.RootActivityComponent
import io.novafoundation.nova.app.root.presentation.main.di.MainFragmentComponent
import io.novafoundation.nova.app.root.presentation.splitScreen.di.SplitScreenFragmentComponent
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.navigation.DelayedNavigationRouter
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
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
import io.novafoundation.nova.feature_push_notifications.di.PushNotificationsFeatureApi
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_versions_api.di.VersionsFeatureApi
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.feature_wallet_connect_api.di.WalletConnectFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi

@Component(
    dependencies = [
        RootDependencies::class
    ],
    modules = [
        RootFeatureModule::class
    ]
)
@FeatureScope
interface RootComponent {

    fun mainActivityComponentFactory(): RootActivityComponent.Factory

    fun splitScreenFragmentComponentFactory(): SplitScreenFragmentComponent.Factory

    fun mainFragmentComponentFactory(): MainFragmentComponent.Factory

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance splitScreenNavigationHolder: SplitScreenNavigationHolder,
            @BindsInstance rootNavigationHolder: RootNavigationHolder,
            @BindsInstance rootRouter: RootRouter,
            @BindsInstance governanceRouter: GovernanceRouter,
            @BindsInstance dAppRouter: DAppRouter,
            @BindsInstance assetsRouter: AssetsRouter,
            @BindsInstance accountRouter: AccountRouter,
            @BindsInstance stakingRouter: StakingRouter,
            @BindsInstance stakingDashboardNavigator: StakingDashboardNavigator,
            @BindsInstance delayedNavigationRouter: DelayedNavigationRouter,
            deps: RootDependencies
        ): RootComponent
    }

    @Component(
        dependencies = [
            AccountFeatureApi::class,
            WalletFeatureApi::class,
            StakingFeatureApi::class,
            CrowdloanFeatureApi::class,
            AssetsFeatureApi::class,
            CurrencyFeatureApi::class,
            GovernanceFeatureApi::class,
            DAppFeatureApi::class,
            DbApi::class,
            CommonApi::class,
            RuntimeApi::class,
            VersionsFeatureApi::class,
            WalletConnectFeatureApi::class,
            PushNotificationsFeatureApi::class,
            DeepLinkingFeatureApi::class,
            LedgerFeatureApi::class,
            BuyFeatureApi::class,
            DeepLinkingFeatureApi::class
        ]
    )
    interface RootFeatureDependenciesComponent : RootDependencies
}
