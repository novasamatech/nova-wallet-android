package io.novafoundation.nova.feature_account_impl.presentation.multisig.operations.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.multisig.operations.MultisigPendingOperationsFragment

@Subcomponent(
    modules = [
        MultisigPendingOperationsModule::class
    ]
)
@ScreenScope
interface MultisigPendingOperationsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): MultisigPendingOperationsComponent
    }

    fun inject(fragment: MultisigPendingOperationsFragment)
}
