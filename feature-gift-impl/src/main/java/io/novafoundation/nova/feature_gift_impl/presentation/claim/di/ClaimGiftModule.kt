package io.novafoundation.nova.feature_gift_impl.presentation.claim.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.interfaces.FileProvider
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_gift_impl.domain.ShareGiftInteractor
import io.novafoundation.nova.feature_gift_impl.presentation.GiftRouter
import io.novafoundation.nova.feature_gift_impl.presentation.claim.ClaimGiftViewModel
import io.novafoundation.nova.feature_gift_impl.presentation.common.PackingGiftAnimationFactory
import io.novafoundation.nova.feature_gift_impl.presentation.share.ShareGiftPayload
import io.novafoundation.nova.feature_gift_impl.presentation.share.ShareGiftViewModel
import io.novafoundation.nova.feature_gift_impl.presentation.share.deeplink.ShareGiftDeepLinkConfigurator
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class ClaimGiftModule {

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): ClaimGiftViewModel {
        return ViewModelProvider(fragment, factory).get(ClaimGiftViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(ClaimGiftViewModel::class)
    fun provideViewModel(
        router: GiftRouter,
        payload: ShareGiftPayload,
        shareGiftInteractor: ShareGiftInteractor,
        chainRegistry: ChainRegistry,
        packingGiftAnimationFactory: PackingGiftAnimationFactory,
        assetIconProvider: AssetIconProvider,
        tokenFormatter: TokenFormatter,
        shareGiftDeepLinkConfigurator: ShareGiftDeepLinkConfigurator,
        fileProvider: FileProvider,
        resourceManager: ResourceManager
    ): ViewModel {
        return ClaimGiftViewModel(
            router = router,
            payload = payload,
            shareGiftInteractor = shareGiftInteractor,
            chainRegistry = chainRegistry,
            packingGiftAnimationFactory = packingGiftAnimationFactory,
            assetIconProvider = assetIconProvider,
            tokenFormatter = tokenFormatter,
            shareGiftDeepLinkConfigurator = shareGiftDeepLinkConfigurator,
            fileProvider = fileProvider,
            resourceManager = resourceManager
        )
    }
}
