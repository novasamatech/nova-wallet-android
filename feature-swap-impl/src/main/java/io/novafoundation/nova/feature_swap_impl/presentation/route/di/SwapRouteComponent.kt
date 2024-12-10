package io.novafoundation.nova.feature_swap_impl.presentation.route.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_swap_impl.presentation.route.SwapRouteFragment

@Subcomponent(
    modules = [
        SwapRouteModule::class
    ]
)
@ScreenScope
interface SwapRouteComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): SwapRouteComponent
    }

    fun inject(fragment: SwapRouteFragment)
}
