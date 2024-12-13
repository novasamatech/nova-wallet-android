package io.novafoundation.nova.feature_assets.presentation.receive.flow.network.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.flow.network.NetworkFlowPayload
import io.novafoundation.nova.feature_assets.presentation.receive.flow.network.NetworkReceiveFlowFragment

@Subcomponent(
    modules = [
        NetworkReceiveFlowModule::class
    ]
)
@ScreenScope
interface NetworkReceiveFlowComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance networkFlowPayload: NetworkFlowPayload
        ): NetworkReceiveFlowComponent
    }

    fun inject(fragment: NetworkReceiveFlowFragment)
}
