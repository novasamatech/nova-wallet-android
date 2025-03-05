package io.novafoundation.nova.feature_banners_impl.di

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope

import javax.inject.Inject

@ApplicationScope
class BannersFeatureHolder @Inject constructor(
    featureContainer: FeatureContainer
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val accountFeatureDependencies = DaggerBannersFeatureComponent_BannersFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .build()

        return DaggerBannersFeatureComponent.factory()
            .create(deps = accountFeatureDependencies)
    }
}
