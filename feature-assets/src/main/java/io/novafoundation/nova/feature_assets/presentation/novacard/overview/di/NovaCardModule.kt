package io.novafoundation.nova.feature_assets.presentation.novacard.overview.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.browser.fileChoosing.WebViewFileChooser
import io.novafoundation.nova.common.utils.browser.fileChoosing.WebViewFileChooserFactory
import io.novafoundation.nova.common.utils.browser.permissions.WebViewPermissionAsker
import io.novafoundation.nova.common.utils.browser.permissions.WebViewPermissionAskerFactory
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_assets.BuildConfig
import io.novafoundation.nova.feature_assets.domain.novaCard.NovaCardInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.NovaCardViewModel
import io.novafoundation.nova.common.utils.webView.BaseWebChromeClientFactory
import io.novafoundation.nova.common.utils.webView.InterceptingWebViewClientFactory
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController.NovaCardWebViewControllerFactory
import io.novafoundation.nova.feature_assets.presentation.novacard.overview.webViewController.interceptors.CardCreationInterceptorFactory
import io.novafoundation.nova.feature_assets.presentation.topup.TopUpAddressCommunicator
import io.novafoundation.nova.feature_buy_api.presentation.trade.interceptors.mercuryo.MercuryoSellRequestInterceptorFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import okhttp3.OkHttpClient

@Module(includes = [ViewModelModule::class])
class NovaCardModule {

    @Provides
    fun providePermissionAsker(fragment: Fragment, factory: WebViewPermissionAskerFactory) = factory.create(fragment)

    @Provides
    fun provideFileChooser(fragment: Fragment, factory: WebViewFileChooserFactory) = factory.create(fragment)

    @Provides
    fun provideCardCreationInterceptorFactory(
        gson: Gson,
        okHttpClient: OkHttpClient
    ): CardCreationInterceptorFactory = CardCreationInterceptorFactory(
        gson = gson,
        okHttpClient = okHttpClient
    )

    @Provides
    fun provideBaseWebChromeClientFactory(
        permissionsAsker: WebViewPermissionAsker,
        webViewFileChooser: WebViewFileChooser
    ) = BaseWebChromeClientFactory(permissionsAsker, webViewFileChooser)

    @Provides
    fun provideNovaCardWebViewControllerFactory(
        appLinksProvider: AppLinksProvider,
        interceptingWebViewClientFactory: InterceptingWebViewClientFactory,
        novaCardWebChromeClientFactory: BaseWebChromeClientFactory
    ): NovaCardWebViewControllerFactory {
        return NovaCardWebViewControllerFactory(
            interceptingWebViewClientFactory,
            novaCardWebChromeClientFactory,
            appLinksProvider,
            BuildConfig.NOVA_CARD_WIDGET_ID
        )
    }

    @Provides
    @IntoMap
    @ViewModelKey(NovaCardViewModel::class)
    fun provideViewModel(
        chainRegistry: ChainRegistry,
        accountInteractor: AccountInteractor,
        assetsRouter: AssetsRouter,
        novaCardInteractor: NovaCardInteractor,
        cardCreationInterceptorFactory: CardCreationInterceptorFactory,
        mercuryoSellRequestInterceptorFactory: MercuryoSellRequestInterceptorFactory,
        novaCardWebViewControllerFactory: NovaCardWebViewControllerFactory,
        topUpAddressCommunicator: TopUpAddressCommunicator,
        resourceManager: ResourceManager
    ): ViewModel {
        return NovaCardViewModel(
            chainRegistry = chainRegistry,
            accountInteractor = accountInteractor,
            assetsRouter = assetsRouter,
            novaCardInteractor = novaCardInteractor,
            cardCreationInterceptorFactory = cardCreationInterceptorFactory,
            mercuryoSellRequestInterceptorFactory = mercuryoSellRequestInterceptorFactory,
            novaCardWebViewControllerFactory = novaCardWebViewControllerFactory,
            topUpRequester = topUpAddressCommunicator,
            resourceManager = resourceManager
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): NovaCardViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(NovaCardViewModel::class.java)
    }
}
