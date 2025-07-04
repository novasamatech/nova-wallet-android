package io.novafoundation.nova.feature_multisig_operations.presentation.enterCall.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_multisig_operations.presentation.enterCall.MultisigOperationEnterCallFragment
import io.novafoundation.nova.feature_multisig_operations.presentation.enterCall.MultisigOperationEnterCallPayload

@Subcomponent(
    modules = [
        MultisigOperationEnterCallModule::class
    ]
)
@ScreenScope
interface MultisigOperationEnterCallComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: MultisigOperationEnterCallPayload,
        ): MultisigOperationEnterCallComponent
    }

    fun inject(fragment: MultisigOperationEnterCallFragment)
}
