package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.CreateCloudBackupPasswordFragment
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.CreateCloudBackupPasswordPayload

@Subcomponent(
    modules = [
        CreateCloudBackupPasswordModule::class
    ]
)
@ScreenScope
interface CreateCloudBackupPasswordComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: CreateCloudBackupPasswordPayload
        ): CreateCloudBackupPasswordComponent
    }

    fun inject(fragment: CreateCloudBackupPasswordFragment)
}
