package io.novafoundation.nova.feature_multisig_operations.presentation.enterCall.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_multisig_operations.presentation.common.MultisigOperationPayload
import io.novafoundation.nova.feature_multisig_operations.presentation.enterCall.MultisigOperationEnterCallFragment

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
            @BindsInstance payload: MultisigOperationPayload,
        ): MultisigOperationEnterCallComponent
    }

    fun inject(fragment: MultisigOperationEnterCallFragment)
}
