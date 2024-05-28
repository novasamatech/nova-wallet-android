package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.syncWallets.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.syncWallets.SyncWalletsBackupPasswordFragment

@Subcomponent(
    modules = [
        SyncWalletsBackupPasswordModule::class
    ]
)
@ScreenScope
interface SyncWalletsBackupPasswordComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): SyncWalletsBackupPasswordComponent
    }

    fun inject(fragment: SyncWalletsBackupPasswordFragment)
}
