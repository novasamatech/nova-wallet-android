package io.novafoundation.nova.feature_pay_impl.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_pay_api.di.PayFeatureApi
import io.novafoundation.nova.feature_pay_impl.presentation.PayRouter
import io.novafoundation.nova.feature_pay_impl.presentation.main.di.PayMainComponent
import io.novafoundation.nova.feature_pay_impl.presentation.shop.di.ShopComponent
import io.novafoundation.nova.feature_wallet_connect_api.di.WalletConnectFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi

@Component(
    dependencies = [
        PayFeatureDependencies::class
    ],
    modules = [
        PayFeatureModule::class
    ]
)
@FeatureScope
interface PayFeatureComponent : PayFeatureApi {

    fun mainPayComponentFactory(): PayMainComponent.Factory

    fun shopComponentFactory(): ShopComponent.Factory

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance router: PayRouter,
            deps: PayFeatureDependencies
        ): PayFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            RuntimeApi::class,
            AccountFeatureApi::class,
            WalletConnectFeatureApi::class
        ]
    )
    interface PayFeatureDependenciesComponent : PayFeatureDependencies
}
