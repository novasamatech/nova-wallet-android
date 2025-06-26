package io.novafoundation.nova.feature_assets.presentation.trade.buy.flow.network.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.trade.buy.flow.network.NetworkBuyFlowFragment
import io.novafoundation.nova.feature_assets.presentation.flow.network.NetworkFlowPayload

@Subcomponent(
    modules = [
        NetworkBuyFlowModule::class
    ]
)
@ScreenScope
interface NetworkBuyFlowComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance networkFlowPayload: NetworkFlowPayload
        ): NetworkBuyFlowComponent
    }

    fun inject(fragment: NetworkBuyFlowFragment)
}
