package io.novafoundation.nova.feature_assets.presentation.buy.flow.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.buy.flow.AssetBuyFlowFragment
import io.novafoundation.nova.feature_assets.presentation.send.flow.di.AssetBuyFlowModule

@Subcomponent(
    modules = [
        AssetBuyFlowModule::class
    ]
)
@ScreenScope
interface AssetBuyFlowComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): AssetBuyFlowComponent
    }

    fun inject(fragment: AssetBuyFlowFragment)
}
