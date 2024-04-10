package io.novafoundation.nova.feature_cloud_backup_impl.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_cloud_backup_api.di.CloudBackupFeatureApi
import io.novafoundation.nova.feature_cloud_backup_impl.presentation.CloudBackupRouter
import io.novafoundation.nova.feature_cloud_backup_impl.presentation.settings.main.di.CloudBackupSettingsComponent
import io.novafoundation.nova.runtime.di.RuntimeApi

@Component(
    dependencies = [
        CloudBackupFeatureDependencies::class
    ],
    modules = [
        CloudBackupFeatureModule::class,
    ]
)
@FeatureScope
interface CloudBackupFeatureComponent : CloudBackupFeatureApi {

    fun cloudBackupSettings(): CloudBackupSettingsComponent.Factory

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance accountRouter: CloudBackupRouter,
            deps: CloudBackupFeatureDependencies
        ): CloudBackupFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            DbApi::class,
            RuntimeApi::class
        ]
    )
    interface CloudBackupFeatureDependenciesComponent : CloudBackupFeatureDependencies
}
