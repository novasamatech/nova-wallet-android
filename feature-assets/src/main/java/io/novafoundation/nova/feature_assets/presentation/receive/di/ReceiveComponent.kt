package io.novafoundation.nova.feature_assets.presentation.receive.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.AssetPayload
import io.novafoundation.nova.feature_assets.presentation.receive.ReceiveFragment
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

@Subcomponent(
    modules = [
        ReceiveModule::class
    ]
)
@ScreenScope
interface ReceiveComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: AssetPayload?,
            @BindsInstance chainId: ChainId?
        ): ReceiveComponent
    }

    fun inject(fragment: ReceiveFragment)
}
