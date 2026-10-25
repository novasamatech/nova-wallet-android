package io.novafoundation.nova.feature_swap_impl.presentation.options.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_swap_impl.presentation.options.SwapOptionsFragment

@Subcomponent(
    modules = [
        SwapOptionsModule::class
    ]
)
@ScreenScope
interface SwapOptionsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): SwapOptionsComponent
    }

    fun inject(fragment: SwapOptionsFragment)
}
