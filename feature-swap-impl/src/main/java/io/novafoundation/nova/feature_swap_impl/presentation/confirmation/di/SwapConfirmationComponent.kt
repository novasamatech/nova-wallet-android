package io.novafoundation.nova.feature_swap_impl.presentation.confirmation.di

import io.novafoundation.nova.feature_swap_api.presentation.model.SwapOperationPayload
import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_swap_impl.presentation.confirmation.SwapConfirmationFragment

@Subcomponent(
    modules = [
        SwapConfirmationModule::class
    ]
)
@ScreenScope
interface SwapConfirmationComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: SwapOperationPayload,
        ): SwapConfirmationComponent
    }

    fun inject(fragment: SwapConfirmationFragment)
}
