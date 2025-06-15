package io.novafoundation.nova.feature_multisig_operations.presentation.created.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_multisig_operations.presentation.created.MultisigCreatedBottomSheet

@Subcomponent(
    modules = [
        MultisigCreatedModule::class
    ]
)
@ScreenScope
interface MultisigCreatedComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(@BindsInstance fragment: Fragment): MultisigCreatedComponent
    }

    fun inject(fragment: MultisigCreatedBottomSheet)
}
