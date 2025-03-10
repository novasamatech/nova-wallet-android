package io.novafoundation.nova.feature_swap_impl.presentation.fee.di

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
        ): SwapFeeComponent
    }

    fun inject(fragment: SwapFeeFragment)
}
