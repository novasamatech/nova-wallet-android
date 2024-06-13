package io.novafoundation.nova.feature_ledger_impl.presentation.account.sign.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_ledger_impl.presentation.account.sign.SignLedgerFragment
import io.novafoundation.nova.feature_ledger_impl.presentation.account.sign.SignLedgerPayload

@Subcomponent(
    modules = [
        SignLedgerModule::class
    ]
)
@ScreenScope
interface SignLedgerComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance request: SignLedgerPayload,
        ): SignLedgerComponent
    }

    fun inject(fragment: SignLedgerFragment)
}
