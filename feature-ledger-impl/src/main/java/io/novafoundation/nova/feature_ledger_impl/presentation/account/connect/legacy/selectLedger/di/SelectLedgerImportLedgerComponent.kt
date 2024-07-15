package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.selectLedger.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_ledger_impl.presentation.account.common.selectLedger.SelectLedgerPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.selectLedger.SelectLedgerImportFragment

@Subcomponent(
    modules = [
        SelectLedgerImportLedgerModule::class
    ]
)
@ScreenScope
interface SelectLedgerImportLedgerComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: SelectLedgerPayload,
        ): SelectLedgerImportLedgerComponent
    }

    fun inject(fragment: SelectLedgerImportFragment)
}
