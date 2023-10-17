package io.novafoundation.nova.feature_assets.presentation.swap.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.send.flow.AssetSendFlowFragment
import io.novafoundation.nova.feature_assets.presentation.swap.AssetSwapFlowFragment
import io.novafoundation.nova.feature_assets.presentation.swap.SwapFlowPayload

@Subcomponent(
    modules = [
        AssetSwapFlowModule::class
    ]
)
@ScreenScope
interface AssetSwapFlowComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: SwapFlowPayload
        ): AssetSwapFlowComponent
    }

    fun inject(fragment: AssetSwapFlowFragment)
}
