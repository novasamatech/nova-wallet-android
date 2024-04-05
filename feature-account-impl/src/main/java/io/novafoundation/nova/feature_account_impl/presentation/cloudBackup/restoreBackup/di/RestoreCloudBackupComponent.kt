package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.restoreBackup.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.restoreBackup.RestoreCloudBackupFragment

@Subcomponent(
    modules = [
        RestoreCloudBackupModule::class
    ]
)
@ScreenScope
interface RestoreCloudBackupComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): RestoreCloudBackupComponent
    }

    fun inject(fragment: RestoreCloudBackupFragment)
}
