package io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.selectLedger.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.AddChainAccountSelectLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.addChain.selectLedger.AddChainAccountSelectLedgerFragment

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
