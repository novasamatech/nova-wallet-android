package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.fillWallet.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.fillWallet.FillWalletImportLedgerFragment

@Subcomponent(
    modules = [
        FillWalletImportLedgerModule::class
    ]
)
@ScreenScope
interface FillWalletImportLedgerComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): FillWalletImportLedgerComponent
    }

    fun inject(fragment: FillWalletImportLedgerFragment)
}
