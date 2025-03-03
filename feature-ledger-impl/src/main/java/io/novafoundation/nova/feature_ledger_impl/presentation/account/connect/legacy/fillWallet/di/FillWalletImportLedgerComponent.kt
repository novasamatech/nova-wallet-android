package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.fillWallet.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.fillWallet.FillWalletImportLedgerFragment
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.fillWallet.FillWalletImportLedgerLegacyPayload

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
            @BindsInstance payload: FillWalletImportLedgerLegacyPayload
        ): FillWalletImportLedgerComponent
    }

    fun inject(fragment: FillWalletImportLedgerFragment)
}
