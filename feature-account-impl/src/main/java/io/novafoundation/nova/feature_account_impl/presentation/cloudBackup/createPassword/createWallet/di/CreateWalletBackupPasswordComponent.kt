package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.createWallet.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.createWallet.CreateWalletBackupPasswordFragment
import io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword.createWallet.CreateBackupPasswordPayload

@Subcomponent(
    modules = [
        CreateWalletBackupPasswordModule::class
    ]
)
@ScreenScope
interface CreateWalletBackupPasswordComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: CreateBackupPasswordPayload
        ): CreateWalletBackupPasswordComponent
    }

    fun inject(fragment: CreateWalletBackupPasswordFragment)
}
