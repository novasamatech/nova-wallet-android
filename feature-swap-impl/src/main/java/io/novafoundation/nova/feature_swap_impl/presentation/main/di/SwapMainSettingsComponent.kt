package io.novafoundation.nova.feature_swap_impl.presentation.main.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_swap_impl.presentation.main.SwapMainSettingsFragment

@Subcomponent(
    modules = [
        SwapMainSettingsModule::class
    ]
)
@ScreenScope
interface SwapMainSettingsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): SwapMainSettingsComponent
    }

    fun inject(fragment: SwapMainSettingsFragment)
}
