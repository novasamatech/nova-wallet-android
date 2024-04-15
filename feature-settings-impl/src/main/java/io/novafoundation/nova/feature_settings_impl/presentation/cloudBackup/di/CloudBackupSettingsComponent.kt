package io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup.CloudBackupSettingsFragment

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
