package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.ManualBackupSelectWalletFragment

@Subcomponent(
    modules = [
        ManualBackupSelectWalletModule::class
    ]
)
@ScreenScope
interface ManualBackupSelectWalletComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): ManualBackupSelectWalletComponent
    }

    fun inject(fragment: ManualBackupSelectWalletFragment)
}
