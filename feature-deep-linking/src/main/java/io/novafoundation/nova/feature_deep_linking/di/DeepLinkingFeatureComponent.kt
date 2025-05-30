package io.novafoundation.nova.feature_deep_linking.di

import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope

@Component(
    dependencies = [
        DeepLinkingFeatureDependencies::class
    ],
    modules = [
        DeepLinkingFeatureModule::class
    ]
)
@FeatureScope
interface DeepLinkingFeatureComponent : DeepLinkingFeatureApi {

    @Component.Factory
    interface Factory {

        fun create(
            deps: DeepLinkingFeatureDependencies
        ): DeepLinkingFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class
        ]
    )
    interface DeepLinkingFeatureDependenciesComponent : DeepLinkingFeatureDependencies
}
