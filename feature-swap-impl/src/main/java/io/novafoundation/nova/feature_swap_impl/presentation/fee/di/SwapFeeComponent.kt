package io.novafoundation.nova.feature_swap_impl.presentation.fee.di

import io.novafoundation.nova.feature_swap_api.presentation.model.SwapOperationPayload
import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_swap_impl.presentation.fee.SwapFeeFragment

@Subcomponent(
    modules = [
        SwapFeeModule::class
    ]
)
@ScreenScope
interface SwapFeeComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: SwapOperationPayload,
        ): SwapFeeComponent
    }

    fun inject(fragment: SwapFeeFragment)
}
