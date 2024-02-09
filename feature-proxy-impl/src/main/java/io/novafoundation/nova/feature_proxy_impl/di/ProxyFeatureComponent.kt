package io.novafoundation.nova.feature_proxy_impl.di

import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_proxy_api.di.ProxyFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi

@Component(
    dependencies = [
        ProxyFeatureDependencies::class
    ],
    modules = [
        ProxyFeatureModule::class,
    ]
)
@FeatureScope
interface ProxyFeatureComponent : ProxyFeatureApi {

    @Component.Factory
    interface Factory {

        fun create(
            deps: ProxyFeatureDependencies
        ): ProxyFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            RuntimeApi::class
        ]
    )
    interface VoteFeatureDependenciesComponent : ProxyFeatureDependencies
}
