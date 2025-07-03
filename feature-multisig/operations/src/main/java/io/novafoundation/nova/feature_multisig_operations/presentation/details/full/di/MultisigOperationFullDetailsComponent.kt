package io.novafoundation.nova.feature_multisig_operations.presentation.details.full.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_multisig_operations.presentation.details.common.MultisigOperationDetailsPayload
import io.novafoundation.nova.feature_multisig_operations.presentation.details.full.MultisigOperationFullDetailsFragment

@Subcomponent(
    modules = [
        MultisigOperationFullDetailsModule::class
    ]
)
@ScreenScope
interface MultisigOperationFullDetailsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: MultisigOperationDetailsPayload,
        ): MultisigOperationFullDetailsComponent
    }

    fun inject(fragment: MultisigOperationFullDetailsFragment)
}
