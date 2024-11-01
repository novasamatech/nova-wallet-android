package io.novafoundation.nova.feature_assets.presentation.send.flow.asset.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.send.flow.asset.AssetSendFlowFragment

@Subcomponent(
    modules = [
        AssetSendFlowModule::class
    ]
)
@ScreenScope
interface AssetSendFlowComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): AssetSendFlowComponent
    }

    fun inject(fragment: AssetSendFlowFragment)
}
