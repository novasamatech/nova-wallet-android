package io.novafoundation.nova.feature_wallet_impl.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.feature_wallet_api.presentation.WalletRouter
import io.novafoundation.nova.feature_wallet_impl.di.modules.AssetsModule
import io.novafoundation.nova.feature_wallet_impl.di.modules.BalanceLocksModule
import io.novafoundation.nova.feature_wallet_impl.di.modules.ValidationsModule
import io.novafoundation.nova.runtime.di.RuntimeApi

@Component(
    dependencies = [
        WalletFeatureDependencies::class
    ],
    modules = [
        WalletFeatureModule::class,
        ValidationsModule::class,
        AssetsModule::class,
        BalanceLocksModule::class,
    ]
)
@FeatureScope
interface WalletFeatureComponent : WalletFeatureApi {

    fun selectCurrencyComponentFactory(): SelectCurrencyComponent.Factory

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance walletRouter: WalletRouter,
            deps: WalletFeatureDependencies
        ): WalletFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            DbApi::class,
            RuntimeApi::class,
            AccountFeatureApi::class
        ]
    )
    interface WalletFeatureDependenciesComponent : WalletFeatureDependencies
}
