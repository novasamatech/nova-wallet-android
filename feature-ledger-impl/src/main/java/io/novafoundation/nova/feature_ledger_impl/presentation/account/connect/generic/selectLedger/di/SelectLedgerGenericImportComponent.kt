package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.selectLedger.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.selectLedger.SelectLedgerGenericImportFragment

@Subcomponent(
    modules = [
        SelectLedgerGenericImportModule::class
    ]
)
@ScreenScope
interface SelectLedgerGenericImportComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): SelectLedgerGenericImportComponent
    }

    fun inject(fragment: SelectLedgerGenericImportFragment)
}
