package io.novafoundation.nova.feature_account_migration.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_migration.presentation.AccountMigrationRouter
import io.novafoundation.nova.feature_account_migration.presentation.pairing.di.AccountMigrationPairingComponent
import io.novafoundation.nova.feature_cloud_backup_api.di.CloudBackupFeatureApi

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

    fun accountMigrationPairingComponentFactory(): AccountMigrationPairingComponent.Factory

    @Component.Factory
    interface Factory {

        fun create(
            deps: AccountMigrationFeatureDependencies,
            @BindsInstance router: AccountMigrationRouter
        ): AccountMigrationFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            AccountFeatureApi::class,
            CloudBackupFeatureApi::class
        ]
    )
    interface AccountMigrationFeatureDependenciesComponent : AccountMigrationFeatureDependencies
}
