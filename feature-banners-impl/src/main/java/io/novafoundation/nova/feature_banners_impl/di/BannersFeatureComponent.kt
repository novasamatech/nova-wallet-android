package io.novafoundation.nova.feature_banners_impl.di

import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_banners_api.di.BannersFeatureApi

@Component(
    dependencies = [
        BannersFeatureDependencies::class,
    ],
    modules = [
        BannersFeatureModule::class
    ]
)
@FeatureScope
interface BannersFeatureComponent : BannersFeatureApi {

    @Component.Factory
    interface Factory {

        fun create(deps: BannersFeatureDependencies): BannersFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class
        ]
    )
    interface BannersFeatureDependenciesComponent : BannersFeatureDependencies
}
