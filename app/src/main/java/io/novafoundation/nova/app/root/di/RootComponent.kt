package io.novafoundation.nova.app.root.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.app.root.presentation.RootRouter
import io.novafoundation.nova.app.root.presentation.di.RootActivityComponent
import io.novafoundation.nova.app.root.presentation.main.di.MainFragmentComponent
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_crowdloan_api.di.CrowdloanFeatureApi
import io.novafoundation.nova.feature_currency_api.di.CurrencyFeatureApi
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
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

    fun mainFragmentComponentFactory(): MainFragmentComponent.Factory

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance navigationHolder: NavigationHolder,
            @BindsInstance rootRouter: RootRouter,
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
            DbApi::class,
            CommonApi::class,
            RuntimeApi::class,
            VersionsFeatureApi::class,
            WalletConnectFeatureApi::class,
        ]
    )
    interface RootFeatureDependenciesComponent : RootDependencies
}
