package io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.legacy.selectLedger.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.legacy.selectLedger.AddChainAccountSelectLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.legacy.selectLedger.AddChainAccountSelectLedgerFragment

@Subcomponent(
    modules = [
        AddChainAccountSelectLedgerModule::class
    ]
)
@ScreenScope
interface AddChainAccountSelectLedgerComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: AddChainAccountSelectLedgerPayload,
        ): AddChainAccountSelectLedgerComponent
    }

    fun inject(fragment: AddChainAccountSelectLedgerFragment)
}
