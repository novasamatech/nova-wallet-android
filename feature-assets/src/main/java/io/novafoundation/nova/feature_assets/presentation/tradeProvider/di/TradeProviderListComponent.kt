package io.novafoundation.nova.feature_assets.presentation.tradeProvider.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.tradeProvider.TradeProviderListFragment
import io.novafoundation.nova.feature_assets.presentation.tradeProvider.TradeProviderListPayload

@Subcomponent(
    modules = [
        TradeProviderListModule::class
    ]
)
@ScreenScope
interface TradeProviderListComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: TradeProviderListPayload
        ): TradeProviderListComponent
    }

    fun inject(fragment: TradeProviderListFragment)
}
