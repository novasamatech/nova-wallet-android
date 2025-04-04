package io.novafoundation.nova.feature_assets.presentation.trade.webInterface.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_assets.presentation.trade.webInterface.TradeWebFragment
import io.novafoundation.nova.feature_assets.presentation.trade.webInterface.TradeWebPayload

@Subcomponent(
    modules = [
        TradeWebModule::class
    ]
)
@ScreenScope
interface TradeWebComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: TradeWebPayload
        ): TradeWebComponent
    }

    fun inject(fragment: TradeWebFragment)
}
