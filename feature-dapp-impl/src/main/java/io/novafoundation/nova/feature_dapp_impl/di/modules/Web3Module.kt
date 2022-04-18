package io.novafoundation.nova.feature_dapp_impl.di.modules

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.core_db.dao.DappAuthorizationDao
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor
import io.novafoundation.nova.feature_dapp_impl.domain.browser.polkadotJs.PolkadotJsExtensionInteractor
import io.novafoundation.nova.feature_dapp_impl.web3.Web3Responder
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.PolkadotJsTransportFactory
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.PolkadotJsWeb3Injector
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.di.PolkadotJs
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.states.PolkadotJsStateFactory
import io.novafoundation.nova.feature_dapp_impl.web3.session.DbWeb3Session
import io.novafoundation.nova.feature_dapp_impl.web3.session.Web3Session
import io.novafoundation.nova.feature_dapp_impl.web3.states.ExtensionStoreFactory
import io.novafoundation.nova.feature_dapp_impl.web3.webview.Web3WebViewClientFactory
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewHolder
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewScriptInjector
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewWeb3JavaScriptInterface
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewWeb3Responder
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.RuntimeVersionsRepository

@Module
class Web3Module {

    @Provides
    @FeatureScope
    fun provideWebViewHolder() = WebViewHolder()

    @Provides
    @FeatureScope
    fun provideWeb3JavascriptResponder(webViewHolder: WebViewHolder): Web3Responder {
        return WebViewWeb3Responder(webViewHolder)
    }

    @Provides
    @PolkadotJs
    @FeatureScope
    fun provideWeb3JavaScriptInterface() = WebViewWeb3JavaScriptInterface()

    @Provides
    @FeatureScope
    fun provideScriptInjector(
        resourceManager: ResourceManager,
    ) = WebViewScriptInjector(resourceManager)

    @Provides
    @FeatureScope
    fun providePolkadotJsWeb3Injector(
        webViewScriptInjector: WebViewScriptInjector,
        @PolkadotJs web3JavaScriptInterface: WebViewWeb3JavaScriptInterface,
    ) = PolkadotJsWeb3Injector(web3JavaScriptInterface, webViewScriptInjector)

    @Provides
    @FeatureScope
    fun provideWeb3ClientFactory(
        polkadotJsWeb3injector: PolkadotJsWeb3Injector,
    ) = Web3WebViewClientFactory(
        injectors = listOf(
            polkadotJsWeb3injector
        )
    )

    @Provides
    @FeatureScope
    fun provideWeb3Session(
        dappAuthorizationDao: DappAuthorizationDao
    ): Web3Session = DbWeb3Session(dappAuthorizationDao)

    @Provides
    @FeatureScope
    fun providePolkadotJsTransportFactory(
        web3Responder: Web3Responder,
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
        web3Session: Web3Session
    ): PolkadotJsStateFactory {
        return PolkadotJsStateFactory(
            interactor = interactor,
            commonInteractor = commonInteractor,
            resourceManager = resourceManager,
            addressIconGenerator = addressIconGenerator,
            web3Session = web3Session,
        )
    }


    @Provides
    @FeatureScope
    fun provideExtensionStoreFactory(
        polkadotJsStateFactory: PolkadotJsStateFactory,
        polkadotJsTransportFactory: PolkadotJsTransportFactory,
    ) = ExtensionStoreFactory(polkadotJsStateFactory, polkadotJsTransportFactory)
}
