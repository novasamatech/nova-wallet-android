package io.novafoundation.nova.caip.di

import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope

@Component(
    modules = [
        CaipModule::class
    ],
    dependencies = [
        CaipDependencies::class
    ]
)
@FeatureScope
abstract class CaipFeatureComponent : CaipApi {

    @Component(
        dependencies = [
            CommonApi::class,
        ]
    )
    interface CaipDependenciesComponent : CaipDependencies
}
