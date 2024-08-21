package io.novafoundation.nova.feature_swap_core.di

import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.runtime.di.RuntimeApi

@Component(
    dependencies = [
        SwapCoreDependencies::class,
    ],
    modules = [
        SwapCoreModule::class,
    ]
)
@FeatureScope
interface SwapCoreComponent : SwapCoreApi {

    @Component.Factory
    interface Factory {

        fun create(deps: SwapCoreDependencies): SwapCoreComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            RuntimeApi::class
        ]
    )
    interface SwapCoreDependenciesComponent : SwapCoreDependencies
}
