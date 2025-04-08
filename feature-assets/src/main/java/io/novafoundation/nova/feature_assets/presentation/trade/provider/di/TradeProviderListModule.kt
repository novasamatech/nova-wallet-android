package io.novafoundation.nova.feature_assets.presentation.trade.provider.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.trade.provider.TradeProviderListPayload
import io.novafoundation.nova.feature_assets.presentation.trade.provider.TradeProviderListViewModel
import io.novafoundation.nova.feature_buy_api.presentation.mixin.TradeMixin
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class TradeProviderListModule {

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): TradeProviderListViewModel {
        return ViewModelProvider(fragment, factory).get(TradeProviderListViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(TradeProviderListViewModel::class)
    fun provideViewModel(
        payload: TradeProviderListPayload,
        tradeMixinFactory: TradeMixin.Factory,
        resourceManager: ResourceManager,
        chainRegistry: ChainRegistry,
        router: AssetsRouter
    ): ViewModel {
        return TradeProviderListViewModel(
            payload = payload,
            tradeMixinFactory = tradeMixinFactory,
            resourceManager = resourceManager,
            chainRegistry = chainRegistry,
            router = router
        )
    }
}
