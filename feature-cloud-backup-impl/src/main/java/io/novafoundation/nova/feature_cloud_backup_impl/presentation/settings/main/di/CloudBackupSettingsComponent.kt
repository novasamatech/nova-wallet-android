package io.novafoundation.nova.feature_cloud_backup_impl.presentation.settings.main.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_cloud_backup_impl.presentation.settings.main.CloudBackupSettingsFragment

@Subcomponent(
    modules = [
        CloudBackupSettingsModule::class
    ]
)
@ScreenScope
interface CloudBackupSettingsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): CloudBackupSettingsComponent
    }

    fun inject(fragment: CloudBackupSettingsFragment)
}
