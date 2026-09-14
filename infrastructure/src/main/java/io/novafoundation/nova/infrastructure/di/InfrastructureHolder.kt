package io.novafoundation.nova.infrastructure.di

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import javax.inject.Inject

@ApplicationScope
class InfrastructureHolder @Inject constructor(
    featureContainer: FeatureContainer
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val dependencies = DaggerInfrastructureComponent_InfrastructureDependenciesComponent.builder()
            .commonApi(commonApi())
            .build()
        return DaggerInfrastructureComponent.builder()
            .infrastructureDependencies(dependencies)
            .build()
    }
}
