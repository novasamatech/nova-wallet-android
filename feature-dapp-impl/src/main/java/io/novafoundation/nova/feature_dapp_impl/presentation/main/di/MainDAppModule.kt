package io.novafoundation.nova.feature_dapp_impl.presentation.main.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_banners_api.presentation.PromotionBannersMixinFactory
import io.novafoundation.nova.feature_banners_api.presentation.source.BannersSourceFactory
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor
import io.novafoundation.nova.feature_dapp_impl.presentation.main.MainDAppViewModel
import io.novafoundation.nova.feature_wallet_connect_api.presentation.mixin.WalletConnectSessionsMixinFactory

@Module(includes = [ViewModelModule::class])
class MainDAppModule {

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): MainDAppViewModel {
        return ViewModelProvider(fragment, factory).get(MainDAppViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(MainDAppViewModel::class)
    fun provideViewModel(
        promotionBannersMixinFactory: PromotionBannersMixinFactory,
        bannerSourceFactory: BannersSourceFactory,
        selectedAccountUseCase: SelectedAccountUseCase,
        router: DAppRouter,
        dappInteractor: DappInteractor,
        resourceManager: ResourceManager,
        walletConnectSessionsMixinFactory: WalletConnectSessionsMixinFactory,
    ): ViewModel {
        return MainDAppViewModel(
            promotionBannersMixinFactory = promotionBannersMixinFactory,
            bannerSourceFactory = bannerSourceFactory,
            router = router,
            selectedAccountUseCase = selectedAccountUseCase,
            walletConnectSessionsMixinFactory = walletConnectSessionsMixinFactory,
            dappInteractor = dappInteractor,
            resourceManager = resourceManager
        )
    }
}
