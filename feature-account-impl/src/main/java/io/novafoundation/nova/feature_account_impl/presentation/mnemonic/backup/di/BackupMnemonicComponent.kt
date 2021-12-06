package io.novafoundation.nova.feature_account_impl.presentation.mnemonic.backup.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.backup.BackupMnemonicFragment
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.backup.BackupMnemonicPayload

@Subcomponent(
    modules = [
        BackupMnemonicModule::class
    ]
)
@ScreenScope
interface BackupMnemonicComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: BackupMnemonicPayload,
        ): BackupMnemonicComponent
    }

    fun inject(backupMnemonicFragment: BackupMnemonicFragment)
}
