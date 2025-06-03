package io.novafoundation.nova.feature_deep_linking.di

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import javax.inject.Inject

class DeepLinkingFeatureHolder @Inject constructor(
    featureContainer: FeatureContainer
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val dependencies = DaggerDeepLinkingFeatureComponent_DeepLinkingFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .build()

        return DaggerDeepLinkingFeatureComponent.factory()
            .create(
                deps = dependencies
            )
    }
}
