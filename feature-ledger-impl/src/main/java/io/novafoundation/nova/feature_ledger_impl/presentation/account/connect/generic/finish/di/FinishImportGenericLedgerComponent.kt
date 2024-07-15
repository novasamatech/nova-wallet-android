package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.finish.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.finish.FinishImportGenericLedgerFragment
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.finish.FinishImportGenericLedgerPayload

@Subcomponent(
    modules = [
        FinishImportGenericLedgerModule::class
    ]
)
@ScreenScope
interface FinishImportGenericLedgerComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: FinishImportGenericLedgerPayload,
        ): FinishImportGenericLedgerComponent
    }

    fun inject(fragment: FinishImportGenericLedgerFragment)
}
