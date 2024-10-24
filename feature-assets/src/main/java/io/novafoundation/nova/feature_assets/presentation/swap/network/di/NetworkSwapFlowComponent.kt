package io.novafoundation.nova.feature_assets.presentation.swap.network.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.buy.flow.asset.AssetBuyFlowFragment
import io.novafoundation.nova.feature_assets.presentation.buy.flow.network.NetworkBuyFlowFragment
import io.novafoundation.nova.feature_assets.presentation.flow.network.NetworkFlowPayload
import io.novafoundation.nova.feature_assets.presentation.send.flow.network.NetworkSendFlowFragment
import io.novafoundation.nova.feature_assets.presentation.swap.network.NetworkSwapFlowFragment

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
            @BindsInstance networkFlowPayload: NetworkFlowPayload
        ): NetworkSwapFlowComponent
    }

    fun inject(fragment: NetworkSwapFlowFragment)
}
