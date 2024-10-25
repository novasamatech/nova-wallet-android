package io.novafoundation.nova.feature_assets.presentation.swap.network.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.swap.network.NetworkSwapFlowFragment
import io.novafoundation.nova.feature_assets.presentation.swap.network.NetworkSwapFlowPayload

@Subcomponent(
    modules = [
        NetworkSwapFlowModule::class
    ]
)
@ScreenScope
interface NetworkSwapFlowComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: NetworkSwapFlowPayload
        ): NetworkSwapFlowComponent
    }

    fun inject(fragment: NetworkSwapFlowFragment)
}
