package io.novafoundation.nova.feature_swap_impl.presentation.main.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_swap_impl.presentation.main.SwapMainSettingsFragment
import io.novafoundation.nova.feature_swap_api.presentation.model.SwapSettingsPayload

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
            @BindsInstance payload: SwapSettingsPayload
        ): SwapMainSettingsComponent
    }

    fun inject(fragment: SwapMainSettingsFragment)
}
