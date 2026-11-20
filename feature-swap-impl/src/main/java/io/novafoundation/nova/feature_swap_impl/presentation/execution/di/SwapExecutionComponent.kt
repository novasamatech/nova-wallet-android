package io.novafoundation.nova.feature_swap_impl.presentation.execution.di

import io.novafoundation.nova.feature_swap_api.presentation.model.SwapOperationPayload
import io.novafoundation.nova.feature_swap_impl.presentation.execution.model.SwapExecutionSavedState
import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_swap_impl.presentation.execution.SwapExecutionFragment

@Subcomponent(
    modules = [
        SwapExecutionModule::class
    ]
)
@ScreenScope
interface SwapExecutionComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: SwapOperationPayload,
            @BindsInstance savedState: SwapExecutionSavedState,
        ): SwapExecutionComponent
    }

    fun inject(fragment: SwapExecutionFragment)
}
