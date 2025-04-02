package io.novafoundation.nova.feature_assets.presentation.trade.webInterface.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.browser.fileChoosing.WebViewFileChooser
import io.novafoundation.nova.common.utils.browser.fileChoosing.WebViewFileChooserFactory
import io.novafoundation.nova.common.utils.browser.permissions.WebViewPermissionAsker
import io.novafoundation.nova.common.utils.browser.permissions.WebViewPermissionAskerFactory
import io.novafoundation.nova.common.utils.webView.BaseWebChromeClientFactory
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.trade.webInterface.TradeWebPayload
import io.novafoundation.nova.feature_assets.presentation.trade.webInterface.TradeWebViewModel
import io.novafoundation.nova.feature_buy_api.presentation.mixin.TradeMixin
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module(includes = [ViewModelModule::class])
class TradeWebModule {

    @Provides
    @ScreenScope
    fun provideFileChooser(
        fragment: Fragment,
        webViewFileChooserFactory: WebViewFileChooserFactory
    ) = webViewFileChooserFactory.create(fragment)

    @Provides
    @ScreenScope
    fun providePermissionAsker(
        fragment: Fragment,
        webViewPermissionAskerFactory: WebViewPermissionAskerFactory
    ) = webViewPermissionAskerFactory.create(fragment)

    @Provides
    fun provideBaseWebChromeClientFactory(
        permissionsAsker: WebViewPermissionAsker,
        webViewFileChooser: WebViewFileChooser
    ) = BaseWebChromeClientFactory(permissionsAsker, webViewFileChooser)

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): TradeWebViewModel {
        return ViewModelProvider(fragment, factory).get(TradeWebViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(TradeWebViewModel::class)
    fun provideViewModel(
        payload: TradeWebPayload,
        tradeMixinFactory: TradeMixin.Factory,
        resourceManager: ResourceManager,
        chainRegistry: ChainRegistry,
        router: AssetsRouter,
        accountUseCase: SelectedAccountUseCase,
        baseWebChromeClientFactory: BaseWebChromeClientFactory
    ): ViewModel {
        return TradeWebViewModel(
            payload = payload,
            tradeMixinFactory = tradeMixinFactory,
            resourceManager = resourceManager,
            chainRegistry = chainRegistry,
            router = router,
            accountUseCase = accountUseCase,
            baseWebChromeClientFactory = baseWebChromeClientFactory
        )
    }
}
