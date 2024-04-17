package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.changePassword.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.changePassword.ChangeBackupPasswordFragment

@Subcomponent(
    modules = [
        ChangeBackupPasswordModule::class
    ]
)
@ScreenScope
interface ChangeBackupPasswordComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): ChangeBackupPasswordComponent
    }

    fun inject(fragment: ChangeBackupPasswordFragment)
}
