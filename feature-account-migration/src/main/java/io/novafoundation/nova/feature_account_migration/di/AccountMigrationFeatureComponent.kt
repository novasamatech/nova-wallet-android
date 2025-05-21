package io.novafoundation.nova.feature_account_migration.di

import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi

@Component(
    dependencies = [
        AccountMigrationFeatureDependencies::class
    ],
    modules = [
        AccountMigrationFeatureModule::class
    ]
)
@FeatureScope
interface AccountMigrationFeatureComponent : AccountMigrationFeatureApi {

    @Component.Factory
    interface Factory {

        fun create(
            deps: AccountMigrationFeatureDependencies,
        ): AccountMigrationFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            AccountFeatureApi::class
        ]
    )
    interface AccountMigrationFeatureDependenciesComponent : AccountMigrationFeatureDependencies
}
