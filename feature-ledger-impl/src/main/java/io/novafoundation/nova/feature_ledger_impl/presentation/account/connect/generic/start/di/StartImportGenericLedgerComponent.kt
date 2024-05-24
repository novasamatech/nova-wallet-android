package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.start.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.start.StartImportGenericLedgerFragment

@Subcomponent(
    modules = [
        StartImportGenericLedgerModule::class
    ]
)
@ScreenScope
interface StartImportGenericLedgerComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): StartImportGenericLedgerComponent
    }

    fun inject(fragment: StartImportGenericLedgerFragment)
}
