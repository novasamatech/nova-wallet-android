package io.novafoundation.nova.feature_buy_impl.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_buy_api.di.BuyFeatureApi
import io.novafoundation.nova.feature_buy_impl.presentation.BuyRouter
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi

@Component(
    dependencies = [
        BuyFeatureDependencies::class
    ],
    modules = [
        BuyFeatureModule::class
    ]
)
@FeatureScope
interface BuyFeatureComponent : BuyFeatureApi {

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance router: BuyRouter,
            deps: BuyFeatureDependencies
        ): BuyFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            RuntimeApi::class,
            AccountFeatureApi::class,
            WalletFeatureApi::class,
        ]
    )
    interface BuyFeatureDependenciesComponent : BuyFeatureDependencies
}
