package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.enterPassword.confirmPassword.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.enterPassword.confirmPassword.CheckCloudBackupPasswordFragment

@Subcomponent(
    modules = [
        CheckCloudBackupPasswordModule::class
    ]
)
@ScreenScope
interface CheckCloudBackupPasswordComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): CheckCloudBackupPasswordComponent
    }

    fun inject(fragment: CheckCloudBackupPasswordFragment)
}
