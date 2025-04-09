package io.novafoundation.nova.feature_assets.presentation.trade.sell.flow.network.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.flow.network.NetworkFlowPayload
import io.novafoundation.nova.feature_assets.presentation.trade.sell.flow.network.NetworkSellFlowFragment

@Subcomponent(
    modules = [
        NetworkSellFlowModule::class
    ]
)
@ScreenScope
interface NetworkSellFlowComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance networkFlowPayload: NetworkFlowPayload
        ): NetworkSellFlowComponent
    }

    fun inject(fragment: NetworkSellFlowFragment)
}
