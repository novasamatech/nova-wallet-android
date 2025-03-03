package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.selectLedger.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.selectLedger.SelectLedgerLegacyPayload
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.selectLedger.SelectLedgerLegacyImportFragment

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
            @BindsInstance payload: SelectLedgerLegacyPayload,
        ): SelectLedgerImportLedgerComponent
    }

    fun inject(fragment: SelectLedgerLegacyImportFragment)
}
