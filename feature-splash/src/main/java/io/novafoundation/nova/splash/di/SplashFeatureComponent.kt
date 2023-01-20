package io.novafoundation.nova.splash.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_versions_api.di.VersionsFeatureApi
import io.novafoundation.nova.splash.SplashRouter
import io.novafoundation.nova.splash.presentation.di.SplashComponent

@Component(
    dependencies = [
        SplashFeatureDependencies::class
    ],
    modules = [
        SplashFeatureModule::class
    ]
)
@FeatureScope
interface SplashFeatureComponent : SplashFeatureApi {

    fun splashComponentFactory(): SplashComponent.Factory

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun router(splashRouter: SplashRouter): Builder

        fun withDependencies(deps: SplashFeatureDependencies): Builder

        fun build(): SplashFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            AccountFeatureApi::class,
            VersionsFeatureApi::class
        ]
    )
    interface SplashFeatureDependenciesComponent : SplashFeatureDependencies
}
