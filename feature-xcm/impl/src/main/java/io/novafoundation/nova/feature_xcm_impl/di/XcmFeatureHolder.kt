package io.novafoundation.nova.feature_xcm_impl.di

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.runtime.di.RuntimeApi
import javax.inject.Inject

@ApplicationScope
class XcmFeatureHolder @Inject constructor(
    featureContainer: FeatureContainer,
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val xcmFeatureDependencies = DaggerXcmFeatureComponent_XcmFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .runtimeApi(getFeature(RuntimeApi::class.java))
            .build()

        return DaggerXcmFeatureComponent.factory()
            .create(
                deps = xcmFeatureDependencies
            )
    }
}
