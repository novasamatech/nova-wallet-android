package io.novafoundation.nova.analytics.di

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.infrastructure.di.InfrastructureApi
import javax.inject.Inject

@ApplicationScope
class AnalyticsFeatureHolder @Inject constructor(
    featureContainer: FeatureContainer
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val dependencies = DaggerAnalyticsFeatureComponent_AnalyticsFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .infrastructureApi(getFeature(InfrastructureApi::class.java))
            .dbApi(getFeature(DbApi::class.java))
            .build()
        return DaggerAnalyticsFeatureComponent.builder()
            .analyticsFeatureDependencies(dependencies)
            .build()
    }
}
