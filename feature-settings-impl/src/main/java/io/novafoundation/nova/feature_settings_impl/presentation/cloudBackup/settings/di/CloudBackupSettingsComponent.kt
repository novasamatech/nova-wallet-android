package io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup.settings.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup.settings.BackupSettingsFragment

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

    fun inject(fragment: BackupSettingsFragment)
}
