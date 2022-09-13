package io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.selectAddress.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.selectAddress.AddLedgerChainAccountSelectAddressFragment
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.selectAddress.AddLedgerChainAccountSelectAddressPayload

@Subcomponent(
    modules = [
        AddLedgerChainAccountSelectAddressModule::class
    ]
)
@ScreenScope
interface AddLedgerChainAccountSelectAddressComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: AddLedgerChainAccountSelectAddressPayload,
        ): AddLedgerChainAccountSelectAddressComponent
    }

    fun inject(fragment: AddLedgerChainAccountSelectAddressFragment)
}
