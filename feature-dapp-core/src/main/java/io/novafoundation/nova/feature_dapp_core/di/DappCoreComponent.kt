package io.novafoundation.nova.feature_dapp_core.di

import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.runtime.di.RuntimeApi

@Component(
    dependencies = [
        DAppCoreDependencies::class,
    ],
    modules = [
        DAppCoreFeatureModule::class,
    ]
)
@FeatureScope
interface DappCoreComponent : DAppCoreFeatureApi {

    @Component.Factory
    interface Factory {

        fun create(
            deps: DAppCoreDependencies,
        ): DappCoreComponent
    }

    @Component(
        dependencies = [
            CommonApi::class
        ]
    )
    interface DAppCoreDependenciesComponent : DAppCoreDependencies
}
