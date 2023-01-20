package io.novafoundation.nova.feature_versions_impl.di

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.feature_versions_api.presentation.VersionsRouter
import javax.inject.Inject

@ApplicationScope
class VersionsFeatureHolder @Inject constructor(
    featureContainer: FeatureContainer,
    private val router: VersionsRouter
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val dependencies = DaggerVersionsFeatureComponent_StakingFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .build()

        return DaggerVersionsFeatureComponent.factory()
            .create(
                router = router,
                deps = dependencies
            )
    }
}
