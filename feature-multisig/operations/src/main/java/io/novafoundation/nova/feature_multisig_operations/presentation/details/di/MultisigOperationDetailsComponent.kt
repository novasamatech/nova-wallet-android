package io.novafoundation.nova.feature_multisig_operations.presentation.details.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_multisig_operations.presentation.details.MultisigOperationDetailsFragment
import io.novafoundation.nova.feature_multisig_operations.presentation.details.MultisigOperationDetailsPayload

@Subcomponent(
    modules = [
        MultisigOperationDetailsModule::class
    ]
)
@ScreenScope
interface MultisigOperationDetailsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: MultisigOperationDetailsPayload,
        ): MultisigOperationDetailsComponent
    }

    fun inject(fragment: MultisigOperationDetailsFragment)
}
