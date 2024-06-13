package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.start.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.start.StartImportLedgerFragment

@Subcomponent(
    modules = [
        StartImportLedgerModule::class
    ]
)
@ScreenScope
interface StartImportLedgerComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): StartImportLedgerComponent
    }

    fun inject(fragment: StartImportLedgerFragment)
}
