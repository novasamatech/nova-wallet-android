package io.novafoundation.nova.feature_deep_link_building.di

import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.runtime.di.RuntimeApi

@Component(
    dependencies = [
        DeepLinkBuildingFeatureDependencies::class
    ],
    modules = [
        DeepLinkBuildingFeatureModule::class
    ]
)
@FeatureScope
interface DeepLinkBuildingFeatureComponent : DeepLinkBuildingFeatureApi {

    @Component.Factory
    interface Factory {

        fun create(deps: DeepLinkBuildingFeatureDependencies): DeepLinkBuildingFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            RuntimeApi::class
        ]
    )
    interface DeepLinkingFeatureDependenciesComponent : DeepLinkBuildingFeatureDependencies
}
