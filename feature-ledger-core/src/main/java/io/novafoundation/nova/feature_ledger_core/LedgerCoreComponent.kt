package io.novafoundation.nova.feature_ledger_core

import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_ledger_core.di.LedgerCoreApi
import io.novafoundation.nova.runtime.di.RuntimeApi

@Component(
    dependencies = [
        LedgerCoreDependencies::class,
    ],
    modules = [
        LedgerFeatureModule::class,
    ]
)
@FeatureScope
interface LedgerCoreComponent : LedgerCoreApi {

    @Component.Factory
    interface Factory {

        fun create(
            deps: LedgerCoreDependencies,
        ): LedgerCoreComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            RuntimeApi::class,
        ]
    )
    interface LedgerCoreDependenciesComponent : LedgerCoreDependencies
}
