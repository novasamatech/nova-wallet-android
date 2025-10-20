package io.novafoundation.nova.feature_assets.presentation.gifts.networks.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.flow.network.NetworkFlowPayload
import io.novafoundation.nova.feature_assets.presentation.gifts.networks.NetworkGiftsFlowFragment
import io.novafoundation.nova.feature_assets.presentation.send.flow.network.NetworkSendFlowFragment
import io.novafoundation.nova.feature_assets.presentation.send.flow.network.di.NetworkSendFlowModule

@Subcomponent(
    modules = [
        NetworkGiftsFlowModule::class
    ]
)
@ScreenScope
interface NetworkGiftsFlowComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance networkFlowPayload: NetworkFlowPayload
        ): NetworkGiftsFlowComponent
    }

    fun inject(fragment: NetworkGiftsFlowFragment)
}
