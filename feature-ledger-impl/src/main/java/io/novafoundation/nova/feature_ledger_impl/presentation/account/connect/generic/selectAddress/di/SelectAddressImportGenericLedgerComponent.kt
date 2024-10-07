package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.selectAddress.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectLedgerAddressPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.selectAddress.SelectAddressImportGenericLedgerFragment

@Subcomponent(
    modules = [
        SelectAddressImportGenericLedgerModule::class
    ]
)
@ScreenScope
interface SelectAddressImportGenericLedgerComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: SelectLedgerAddressPayload,
        ): SelectAddressImportGenericLedgerComponent
    }

    fun inject(fragment: SelectAddressImportGenericLedgerFragment)
}
