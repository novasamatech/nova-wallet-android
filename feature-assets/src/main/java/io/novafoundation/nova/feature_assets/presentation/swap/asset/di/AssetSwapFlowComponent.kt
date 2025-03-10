package io.novafoundation.nova.feature_assets.presentation.swap.asset.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.swap.asset.AssetSwapFlowFragment
import io.novafoundation.nova.feature_assets.presentation.swap.asset.SwapFlowPayload

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
