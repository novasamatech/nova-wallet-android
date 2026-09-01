package io.novafoundation.nova.analytics.di

import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.infrastructure.di.InfrastructureApi

@Component(
    modules = [
        AnalyticsFeatureModule::class
    ],
    dependencies = [
        AnalyticsFeatureDependencies::class
    ]
)
@ApplicationScope
abstract class AnalyticsFeatureComponent : AnalyticsFeatureApi {

    @Component(
        dependencies = [
            CommonApi::class,
            InfrastructureApi::class,
            DbApi::class
        ]
    )
    interface AnalyticsFeatureDependenciesComponent : AnalyticsFeatureDependencies
}
