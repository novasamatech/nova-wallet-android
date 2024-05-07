package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.accounts.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.accounts.ManualBackupSelectAccountFragment
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.accounts.ManualBackupSelectAccountPayload

@Subcomponent(
    modules = [
        ManualBackupSelectAccountModule::class
    ]
)
@ScreenScope
interface ManualBackupSelectAccountComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: ManualBackupSelectAccountPayload
        ): ManualBackupSelectAccountComponent
    }

    fun inject(fragment: ManualBackupSelectAccountFragment)
}
