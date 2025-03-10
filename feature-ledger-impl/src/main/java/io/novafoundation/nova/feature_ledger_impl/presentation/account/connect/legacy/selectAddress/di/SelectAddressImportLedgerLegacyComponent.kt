package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.selectAddress.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectAddress.SelectLedgerAddressPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.selectAddress.SelectAddressImportLedgerLegacyFragment

@Subcomponent(
    modules = [
        SelectAddressImportLedgerLegacyModule::class
    ]
)
@ScreenScope
interface SelectAddressImportLedgerLegacyComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: SelectLedgerAddressPayload,
        ): SelectAddressImportLedgerLegacyComponent
    }

    fun inject(fragment: SelectAddressImportLedgerLegacyFragment)
}
