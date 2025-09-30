package io.novafoundation.nova.feature_ahm_impl.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_ahm_api.di.ChainMigrationFeatureApi
import io.novafoundation.nova.feature_ahm_impl.presentation.ChainMigrationRouter
import io.novafoundation.nova.feature_ahm_impl.presentation.migrationDetails.di.ChainMigrationDetailsComponent
import io.novafoundation.nova.feature_banners_api.di.BannersFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi

@Component(
    dependencies = [
        ChainMigrationFeatureDependencies::class,
    ],
    modules = [
        ChainMigrationFeatureModule::class,
    ]
)
@FeatureScope
interface ChainMigrationFeatureComponent : ChainMigrationFeatureApi {

    fun chainMigrationDetailsComponentFactory(): ChainMigrationDetailsComponent.Factory

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance router: ChainMigrationRouter,
            deps: ChainMigrationFeatureDependencies
        ): ChainMigrationFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            DbApi::class,
            RuntimeApi::class,
            BannersFeatureApi::class
        ]
    )
    interface AccountFeatureDependenciesComponent : ChainMigrationFeatureDependencies
}
