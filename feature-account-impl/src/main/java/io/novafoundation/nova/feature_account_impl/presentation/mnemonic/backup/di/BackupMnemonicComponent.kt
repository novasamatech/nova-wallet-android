package io.novafoundation.nova.feature_account_impl.presentation.mnemonic.backup.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.backup.BackupMnemonicFragment

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
            @BindsInstance accountName: String,
            @BindsInstance addAccountPayload: AddAccountPayload
        ): BackupMnemonicComponent
    }

    fun inject(backupMnemonicFragment: BackupMnemonicFragment)
}
