package io.novafoundation.nova.feature_assets.presentation.trade.sell.flow.asset.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.trade.sell.flow.asset.AssetSellFlowFragment

@Subcomponent(
    modules = [
        AssetSellFlowModule::class
    ]
)
@ScreenScope
interface AssetSellFlowComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): AssetSellFlowComponent
    }

    fun inject(fragment: AssetSellFlowFragment)
}
