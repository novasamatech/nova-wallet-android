package io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.generic.selectLedger.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.generic.selectLedger.AddEvmAccountSelectGenericLedgerFragment
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.generic.selectLedger.AddEvmAccountSelectGenericLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.legacy.selectLedger.AddChainAccountSelectLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.legacy.selectLedger.AddChainAccountSelectLedgerFragment

@Subcomponent(
    modules = [
        AddEvmAccountSelectGenericLedgerModule::class
    ]
)
@ScreenScope
interface AddEvmAccountSelectGenericLedgerComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: AddEvmAccountSelectGenericLedgerPayload,
        ): AddEvmAccountSelectGenericLedgerComponent
    }

    fun inject(fragment: AddEvmAccountSelectGenericLedgerFragment)
}
