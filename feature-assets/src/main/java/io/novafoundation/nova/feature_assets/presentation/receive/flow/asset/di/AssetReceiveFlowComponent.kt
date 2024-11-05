package io.novafoundation.nova.feature_assets.presentation.receive.flow.asset.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.receive.flow.asset.AssetReceiveFlowFragment

@Subcomponent(
    modules = [
        AssetReceiveFlowModule::class
    ]
)
@ScreenScope
interface AssetReceiveFlowComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): AssetReceiveFlowComponent
    }

    fun inject(fragment: AssetReceiveFlowFragment)
}
