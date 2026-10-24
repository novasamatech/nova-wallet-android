package io.novafoundation.nova.feature_swap_impl.presentation.options.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_swap_api.presentation.model.SlippageSettingsPayload
import io.novafoundation.nova.feature_swap_impl.presentation.options.SlippageSettingsBottomSheet
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
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: SlippageSettingsPayload?
        ): SwapOptionsComponent
    }

    fun inject(fragment: SwapOptionsFragment)

    fun inject(sheet: SlippageSettingsBottomSheet)
}
