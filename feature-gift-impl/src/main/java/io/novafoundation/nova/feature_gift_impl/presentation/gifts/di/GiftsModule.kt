package io.novafoundation.nova.feature_gift_impl.presentation.gifts.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_gift_impl.domain.GiftsInteractor
import io.novafoundation.nova.feature_gift_impl.presentation.GiftRouter
import io.novafoundation.nova.feature_gift_impl.presentation.gifts.GiftsPayload
import io.novafoundation.nova.feature_gift_impl.presentation.gifts.GiftsViewModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class GiftsModule {

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): GiftsViewModel {
        return ViewModelProvider(fragment, factory).get(GiftsViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(GiftsViewModel::class)
    fun provideViewModel(
        router: GiftRouter,
        appLinksProvider: AppLinksProvider,
        giftsInteractor: GiftsInteractor,
        giftsPayload: GiftsPayload,
        chainRegistry: ChainRegistry,
        tokenFormatter: TokenFormatter,
        assetIconProvider: AssetIconProvider,
        resourceManager: ResourceManager
    ): ViewModel {
        return GiftsViewModel(
            router = router,
            appLinksProvider = appLinksProvider,
            giftsInteractor = giftsInteractor,
            giftsPayload = giftsPayload,
            tokenFormatter = tokenFormatter,
            assetIconProvider = assetIconProvider,
            chainRegistry = chainRegistry,
            resourceManager = resourceManager
        )
    }
}
