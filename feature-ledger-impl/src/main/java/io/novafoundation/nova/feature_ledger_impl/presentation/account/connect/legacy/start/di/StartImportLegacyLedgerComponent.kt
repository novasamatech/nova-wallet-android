package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.start.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.start.StartImportLegacyLedgerFragment

@Subcomponent(
    modules = [
        StartImportLegacyLedgerModule::class
    ]
)
@ScreenScope
interface StartImportLegacyLedgerComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): StartImportLegacyLedgerComponent
    }

    fun inject(fragment: StartImportLegacyLedgerFragment)
}
