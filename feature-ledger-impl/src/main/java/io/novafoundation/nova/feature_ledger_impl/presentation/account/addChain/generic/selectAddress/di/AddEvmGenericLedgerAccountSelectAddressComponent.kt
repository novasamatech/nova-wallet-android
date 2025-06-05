package io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.generic.selectAddress.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.generic.selectAddress.AddEvmGenericLedgerAccountSelectAddressFragment
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.generic.selectAddress.AddEvmGenericLedgerAccountSelectAddressPayload

@Subcomponent(
    modules = [
        AddEvmGenericLedgerAccountSelectAddressModule::class
    ]
)
@ScreenScope
interface AddEvmGenericLedgerAccountSelectAddressComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: AddEvmGenericLedgerAccountSelectAddressPayload,
        ): AddEvmGenericLedgerAccountSelectAddressComponent
    }

    fun inject(fragment: AddEvmGenericLedgerAccountSelectAddressFragment)
}
