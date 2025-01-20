package io.novafoundation.nova.feature_deep_link_building.di

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.runtime.di.RuntimeApi
import javax.inject.Inject

class DeepLinkBuildingFeatureHolder @Inject constructor(
    featureContainer: FeatureContainer
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val dependencies = DaggerDeepLinkBuildingFeatureComponent_DeepLinkingFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .runtimeApi(getFeature(RuntimeApi::class.java))
            .build()

        return DaggerDeepLinkBuildingFeatureComponent.factory()
            .create(deps = dependencies)
    }
}
