package io.novafoundation.nova.feature_multisig_operations.presentation.list.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_multisig_operations.presentation.list.MultisigPendingOperationsFragment

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
