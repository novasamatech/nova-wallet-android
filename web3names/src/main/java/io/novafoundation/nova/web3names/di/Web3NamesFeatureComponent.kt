package io.novafoundation.nova.web3names.di

import dagger.Component
import io.novafoundation.nova.caip.di.CaipApi
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.runtime.di.RuntimeApi

@Component(
    modules = [
        Web3NamesModule::class
    ],
    dependencies = [
        Web3NamesDependencies::class
    ]
)
@FeatureScope
abstract class Web3NamesFeatureComponent : Web3NamesApi {

    @Component(
        dependencies = [
            CommonApi::class,
            DbApi::class,
            RuntimeApi::class,
            CaipApi::class
        ]
    )
    interface Web3NamesDependenciesComponent : Web3NamesDependencies
}
