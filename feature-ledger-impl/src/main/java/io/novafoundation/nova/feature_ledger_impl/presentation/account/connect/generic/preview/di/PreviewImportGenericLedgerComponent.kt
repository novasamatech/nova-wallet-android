package io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.preview.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.preview.PreviewImportGenericLedgerFragment
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.generic.preview.PreviewImportGenericLedgerPayload

@Subcomponent(
    modules = [
        PreviewImportGenericLedgerModule::class
    ]
)
@ScreenScope
interface PreviewImportGenericLedgerComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: PreviewImportGenericLedgerPayload
        ): PreviewImportGenericLedgerComponent
    }

    fun inject(fragment: PreviewImportGenericLedgerFragment)
}
