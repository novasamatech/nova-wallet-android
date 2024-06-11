package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.selectAddress.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectLedgerAddressPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.selectAddress.SelectAddressImportLedgerFragment

@Subcomponent(
    modules = [
        SelectAddressImportLedgerModule::class
    ]
)
@ScreenScope
interface SelectAddressImportLedgerComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: SelectLedgerAddressPayload,
        ): SelectAddressImportLedgerComponent
    }

    fun inject(fragment: SelectAddressImportLedgerFragment)
}
