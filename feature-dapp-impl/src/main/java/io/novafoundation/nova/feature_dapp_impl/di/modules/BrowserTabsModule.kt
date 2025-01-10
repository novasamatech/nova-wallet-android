package io.novafoundation.nova.feature_dapp_impl.di.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.interfaces.FileProvider
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.core_db.dao.BrowserTabsDao
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_dapp_api.data.repository.BrowserTabExternalRepository
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.BrowserTabService
import io.novafoundation.nova.feature_dapp_impl.data.repository.tabs.BrowserTabInternalRepository
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.RealPageSnapshotBuilder
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.RealBrowserTabService
import io.novafoundation.nova.feature_dapp_impl.data.repository.tabs.RealBrowserTabRepository
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.PageSnapshotBuilder
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.RealTabMemoryRestrictionService
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.TabMemoryRestrictionService
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.BrowserTabSessionFactory
import io.novafoundation.nova.feature_dapp_impl.web3.webview.CompoundWeb3Injector

@Module
class BrowserTabsModule {

    @FeatureScope
    @Provides
    fun provideBrowserTabStorage(
        browserTabsDao: BrowserTabsDao,
    ): BrowserTabInternalRepository {
        return RealBrowserTabRepository(browserTabsDao = browserTabsDao)
    }

    @FeatureScope
    @Provides
    fun provideBrowserTabRepository(
        repository: BrowserTabInternalRepository,
    ): BrowserTabExternalRepository {
        return repository
    }

    @FeatureScope
    @Provides
    fun providePageSnapshotBuilder(fileProvider: FileProvider, rootScope: RootScope): PageSnapshotBuilder {
        return RealPageSnapshotBuilder(fileProvider, rootScope)
    }

    @FeatureScope
    @Provides
    fun provideTabMemoryRestrictionService(context: Context): TabMemoryRestrictionService {
        return RealTabMemoryRestrictionService(context)
    }

    @FeatureScope
    @Provides
    fun providePageSessionFactory(
        compoundWeb3Injector: CompoundWeb3Injector,
        contextManager: ContextManager
    ): BrowserTabSessionFactory {
        return BrowserTabSessionFactory(compoundWeb3Injector, contextManager)
    }

    @FeatureScope
    @Provides
    fun provideBrowserTabPoolService(
        accountRepository: AccountRepository,
        dAppMetadataRepository: DAppMetadataRepository,
        browserTabInternalRepository: BrowserTabInternalRepository,
        pageSnapshotBuilder: PageSnapshotBuilder,
        tabMemoryRestrictionService: TabMemoryRestrictionService,
        browserTabSessionFactory: BrowserTabSessionFactory,
        rootScope: RootScope
    ): BrowserTabService {
        return RealBrowserTabService(
            browserTabInternalRepository = browserTabInternalRepository,
            pageSnapshotBuilder = pageSnapshotBuilder,
            tabMemoryRestrictionService = tabMemoryRestrictionService,
            browserTabSessionFactory = browserTabSessionFactory,
            accountRepository = accountRepository,
            dAppMetadataRepository = dAppMetadataRepository,
            rootScope = rootScope
        )
    }
}
