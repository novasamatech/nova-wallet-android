package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.finish.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.finish.FinishImportLedgerFragment
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.finish.FinishImportLedgerPayload

@Subcomponent(
    modules = [
        FinishImportLedgerModule::class
    ]
)
@ScreenScope
interface FinishImportLedgerComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: FinishImportLedgerPayload,
        ): FinishImportLedgerComponent
    }

    fun inject(fragment: FinishImportLedgerFragment)
}
