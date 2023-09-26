package io.novafoundation.nova.feature_swap_impl.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_swap_api.di.SwapFeatureApi
import io.novafoundation.nova.feature_swap_impl.presentation.SwapRouter
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi

@Component(
    dependencies = [
        SwapFeatureDependencies::class,
    ],
    modules = [
        SwapFeatureModule::class,
    ]
)
@FeatureScope
interface SwapFeatureComponent : SwapFeatureApi {

    @Component.Factory
    interface Factory {

        fun create(
            deps: SwapFeatureDependencies,
            @BindsInstance router: SwapRouter,
        ): SwapFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            RuntimeApi::class,
            WalletFeatureApi::class,
            AccountFeatureApi::class,
            DbApi::class,
        ]
    )
    interface SwapFeatureDependenciesComponent : SwapFeatureDependencies
}
