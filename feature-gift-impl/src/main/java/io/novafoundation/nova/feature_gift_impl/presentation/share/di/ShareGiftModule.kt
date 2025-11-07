package io.novafoundation.nova.feature_gift_impl.presentation.share.di

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
import io.novafoundation.nova.feature_gift_impl.presentation.common.PackingGiftAnimationFactory
import io.novafoundation.nova.feature_gift_impl.presentation.share.ShareGiftPayload
import io.novafoundation.nova.feature_gift_impl.presentation.share.ShareGiftViewModel
import io.novafoundation.nova.feature_gift_impl.presentation.claim.deeplink.ClaimGiftDeepLinkConfigurator
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class ShareGiftModule {

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): ShareGiftViewModel {
        return ViewModelProvider(fragment, factory).get(ShareGiftViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(ShareGiftViewModel::class)
    fun provideViewModel(
        router: GiftRouter,
        payload: ShareGiftPayload,
        shareGiftInteractor: ShareGiftInteractor,
        chainRegistry: ChainRegistry,
        packingGiftAnimationFactory: PackingGiftAnimationFactory,
        assetIconProvider: AssetIconProvider,
        tokenFormatter: TokenFormatter,
        claimGiftDeepLinkConfigurator: ClaimGiftDeepLinkConfigurator,
        fileProvider: FileProvider,
        resourceManager: ResourceManager
    ): ViewModel {
        return ShareGiftViewModel(
            router = router,
            payload = payload,
            shareGiftInteractor = shareGiftInteractor,
            chainRegistry = chainRegistry,
            packingGiftAnimationFactory = packingGiftAnimationFactory,
            assetIconProvider = assetIconProvider,
            tokenFormatter = tokenFormatter,
            claimGiftDeepLinkConfigurator = claimGiftDeepLinkConfigurator,
            fileProvider = fileProvider,
            resourceManager = resourceManager
        )
    }
}
