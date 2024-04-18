package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.enterPassword.restorePassword.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.enterPassword.restorePassword.RestoreCloudBackupPasswordFragment

@Subcomponent(
    modules = [
        RestoreCloudBackupPasswordModule::class
    ]
)
@ScreenScope
interface RestoreCloudBackupPasswordComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): RestoreCloudBackupPasswordComponent
    }

    fun inject(fragment: RestoreCloudBackupPasswordFragment)
}
