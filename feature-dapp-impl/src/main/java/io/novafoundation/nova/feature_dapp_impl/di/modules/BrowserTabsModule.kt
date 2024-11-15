package io.novafoundation.nova.feature_dapp_impl.di.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.interfaces.FileProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.core_db.dao.BrowserTabsDao
import io.novafoundation.nova.core_db.dao.DappAuthorizationDao
import io.novafoundation.nova.feature_dapp_impl.di.modules.web3.MetamaskModule
import io.novafoundation.nova.feature_dapp_impl.di.modules.web3.PolkadotJsModule
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.BrowserTabPoolService
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.BrowserTabStorage
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.PageSnapshotBuilder
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.RealBrowserTabPoolService
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.RealBrowserTabStorage
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.states.MetamaskStateFactory
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.transport.MetamaskInjector
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.transport.MetamaskTransportFactory
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.PolkadotJsInjector
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.PolkadotJsTransportFactory
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.states.PolkadotJsStateFactory
import io.novafoundation.nova.feature_dapp_impl.web3.session.DbWeb3Session
import io.novafoundation.nova.feature_dapp_impl.web3.session.Web3Session
import io.novafoundation.nova.feature_dapp_impl.web3.states.ExtensionStoreFactory
import io.novafoundation.nova.feature_dapp_impl.web3.webview.Web3WebViewClientFactory
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewHolder
import io.novafoundation.nova.feature_dapp_impl.web3.webview.WebViewScriptInjector

@Module
class BrowserTabsModule {

    @FeatureScope
    @Provides
    fun provideBrowserTabStorage(
        browserTabsDao: BrowserTabsDao,
    ): BrowserTabStorage {
        return RealBrowserTabStorage(browserTabsDao = browserTabsDao)
    }

    @FeatureScope
    @Provides
    fun providePageSnapshotBuilder(fileProvider: FileProvider): PageSnapshotBuilder {
        return PageSnapshotBuilder(fileProvider)
    }

    @FeatureScope
    @Provides
    fun provideBrowserTabPoolService(
        context: Context,
        browserTabStorage: BrowserTabStorage,
        pageSnapshotBuilder: PageSnapshotBuilder
    ): BrowserTabPoolService {
        return RealBrowserTabPoolService(
            context = context,
            browserTabStorage = browserTabStorage,
            pageSnapshotBuilder = pageSnapshotBuilder
        )
    }
}
