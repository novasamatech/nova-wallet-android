package io.novafoundation.nova.feature_versions_impl.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_versions_api.di.VersionsFeatureApi
import io.novafoundation.nova.feature_versions_api.presentation.VersionsRouter
import io.novafoundation.nova.feature_versions_impl.presentation.update.di.UpdateNotificationsComponent

@Component(
    dependencies = [
        VersionsFeatureDependencies::class
    ],
    modules = [
        VersionsFeatureModule::class
    ]
)
@FeatureScope
interface VersionsFeatureComponent : VersionsFeatureApi {

    fun updateNotificationsFragmentComponentFactory(): UpdateNotificationsComponent.Factory

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance router: VersionsRouter,
            deps: VersionsFeatureDependencies
        ): VersionsFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class
        ]
    )
    interface StakingFeatureDependenciesComponent : VersionsFeatureDependencies
}
