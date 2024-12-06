package io.novafoundation.nova.feature_dapp_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.core_db.dao.DappAuthorizationDao
import io.novafoundation.nova.feature_dapp_impl.di.modules.web3.MetamaskModule
import io.novafoundation.nova.feature_dapp_impl.di.modules.web3.PolkadotJsModule
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.states.MetamaskStateFactory
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.transport.MetamaskInjector
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.transport.MetamaskTransportFactory
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.PolkadotJsInjector
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.PolkadotJsTransportFactory
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.states.PolkadotJsStateFactory
import io.novafoundation.nova.feature_dapp_impl.web3.session.DbWeb3Session
import io.novafoundation.nova.feature_dapp_impl.web3.session.Web3Session
import io.novafoundation.nova.feature_dapp_impl.web3.states.ExtensionStoreFactory
import io.novafoundation.nova.feature_dapp_impl.web3.webview.CompoundWeb3Injector
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewHolder
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewScriptInjector

@Module(includes = [PolkadotJsModule::class, MetamaskModule::class])
class Web3Module {

    @Provides
    @FeatureScope
    fun provideWebViewHolder() = WebViewHolder()

    @Provides
    @FeatureScope
    fun provideScriptInjector(
        resourceManager: ResourceManager,
    ) = WebViewScriptInjector(resourceManager)

    @Provides
    @FeatureScope
    fun provideWeb3InjectorPool(
        polkadotJsInjector: PolkadotJsInjector,
        metamaskInjector: MetamaskInjector,
    ) = CompoundWeb3Injector(
        injectors = listOf(
            polkadotJsInjector,
            metamaskInjector
        )
    )

    @Provides
    @FeatureScope
    fun provideWeb3Session(
        dappAuthorizationDao: DappAuthorizationDao
    ): Web3Session = DbWeb3Session(dappAuthorizationDao)

    @Provides
    @FeatureScope
    fun provideExtensionStoreFactory(
        polkadotJsStateFactory: PolkadotJsStateFactory,
        polkadotJsTransportFactory: PolkadotJsTransportFactory,
        metamaskStateFactory: MetamaskStateFactory,
        metamaskTransportFactory: MetamaskTransportFactory,
    ) = ExtensionStoreFactory(
        polkadotJsStateFactory = polkadotJsStateFactory,
        polkadotJsTransportFactory = polkadotJsTransportFactory,
        metamaskStateFactory = metamaskStateFactory,
        metamaskTransportFactory = metamaskTransportFactory
    )
}
