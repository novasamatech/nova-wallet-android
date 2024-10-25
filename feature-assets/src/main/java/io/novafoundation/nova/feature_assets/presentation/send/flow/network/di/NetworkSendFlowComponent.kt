package io.novafoundation.nova.feature_assets.presentation.send.flow.network.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.flow.network.NetworkFlowPayload
import io.novafoundation.nova.feature_assets.presentation.send.flow.network.NetworkSendFlowFragment

@Subcomponent(
    modules = [
        NetworkSendFlowModule::class
    ]
)
@ScreenScope
interface NetworkSendFlowComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance networkFlowPayload: NetworkFlowPayload
        ): NetworkSendFlowComponent
    }

    fun inject(fragment: NetworkSendFlowFragment)
}
