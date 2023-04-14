package io.novafoundation.nova.feature_dapp_impl.di.modules.web3

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor
import io.novafoundation.nova.feature_dapp_impl.domain.browser.polkadotJs.PolkadotJsExtensionInteractor
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.PolkadotJsInjector
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.PolkadotJsResponder
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.PolkadotJsTransportFactory
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.di.PolkadotJs
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.states.PolkadotJsStateFactory
import io.novafoundation.nova.feature_dapp_impl.web3.session.Web3Session
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewHolder
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewScriptInjector
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewWeb3JavaScriptInterface
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.RuntimeVersionsRepository

@Module
class PolkadotJsModule {

    @Provides
    @PolkadotJs
    @FeatureScope
    fun provideWeb3JavaScriptInterface() = WebViewWeb3JavaScriptInterface()

    @Provides
    @FeatureScope
    fun providePolkadotJsInjector(
        webViewScriptInjector: WebViewScriptInjector,
        @PolkadotJs web3JavaScriptInterface: WebViewWeb3JavaScriptInterface,
    ) = PolkadotJsInjector(web3JavaScriptInterface, webViewScriptInjector)

    @Provides
    @FeatureScope
    fun provideResponder(webViewHolder: WebViewHolder): PolkadotJsResponder {
        return PolkadotJsResponder(webViewHolder)
    }

    @Provides
    @FeatureScope
    fun providePolkadotJsTransportFactory(
        web3Responder: PolkadotJsResponder,
        @PolkadotJs web3JavaScriptInterface: WebViewWeb3JavaScriptInterface,
        gson: Gson
    ): PolkadotJsTransportFactory {
        return PolkadotJsTransportFactory(
            webViewWeb3JavaScriptInterface = web3JavaScriptInterface,
            gson = gson,
            web3Responder = web3Responder,
        )
    }

    @Provides
    @FeatureScope
    fun providePolkadotJsInteractor(
        chainRegistry: ChainRegistry,
        runtimeVersionsRepository: RuntimeVersionsRepository,
        accountRepository: AccountRepository
    ) = PolkadotJsExtensionInteractor(chainRegistry, accountRepository, runtimeVersionsRepository)

    @Provides
    @FeatureScope
    fun providePolkadotJsStateFactory(
        interactor: PolkadotJsExtensionInteractor,
        commonInteractor: DappInteractor,
        resourceManager: ResourceManager,
        addressIconGenerator: AddressIconGenerator,
        web3Session: Web3Session,
        walletUiUseCase: WalletUiUseCase,
    ): PolkadotJsStateFactory {
        return PolkadotJsStateFactory(
            interactor = interactor,
            commonInteractor = commonInteractor,
            resourceManager = resourceManager,
            addressIconGenerator = addressIconGenerator,
            web3Session = web3Session,
            walletUiUseCase = walletUiUseCase
        )
    }
}
