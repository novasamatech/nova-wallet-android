package io.novafoundation.nova.infrastructure.di

import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.ApplicationScope

@Component(
    modules = [
        AttestationModule::class
    ],
    dependencies = [
        InfrastructureDependencies::class
    ]
)
@ApplicationScope
abstract class InfrastructureComponent : InfrastructureApi {

    @Component(
        dependencies = [
            CommonApi::class
        ]
    )
    interface InfrastructureDependenciesComponent : InfrastructureDependencies
}
