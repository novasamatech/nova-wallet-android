package io.novafoundation.nova.core_db.di

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import javax.inject.Inject

@ApplicationScope
class DbHolder @Inject constructor(
    featureContainer: FeatureContainer
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val dbDependencies = DaggerDbComponent_DbDependenciesComponent.builder()
            .commonApi(commonApi())
            .build()
        return DaggerDbComponent.builder()
            .dbDependencies(dbDependencies)
            .build()
    }
}
