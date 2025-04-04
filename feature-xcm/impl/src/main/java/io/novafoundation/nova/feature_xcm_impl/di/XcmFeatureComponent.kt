package io.novafoundation.nova.feature_xcm_impl.di

import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_xcm_api.di.XcmFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi

@Component(
    dependencies = [
        XcmFeatureDependencies::class,
    ],
    modules = [
        XcmFeatureModule::class
    ]
)
@FeatureScope
interface XcmFeatureComponent : XcmFeatureApi {

    @Component.Factory
    interface Factory {

        fun create(
            deps: XcmFeatureDependencies
        ): XcmFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            RuntimeApi::class,
        ]
    )
    interface XcmFeatureDependenciesComponent : XcmFeatureDependencies
}
