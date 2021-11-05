package io.novafoundation.nova.runtime.di

import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.core_db.di.DbApi

@Component(
    modules = [
        RuntimeModule::class,
        ChainRegistryModule::class
    ],
    dependencies = [
        RuntimeDependencies::class
    ]
)
@ApplicationScope
abstract class RuntimeComponent : RuntimeApi {

    @Component(
        dependencies = [
            CommonApi::class,
            DbApi::class,
        ]
    )
    interface RuntimeDependenciesComponent : RuntimeDependencies
}
