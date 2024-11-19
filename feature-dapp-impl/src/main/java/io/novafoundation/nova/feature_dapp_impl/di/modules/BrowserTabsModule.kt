package io.novafoundation.nova.feature_dapp_impl.di.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.interfaces.FileProvider
import io.novafoundation.nova.core_db.dao.BrowserTabsDao
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.BrowserTabPoolService
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.BrowserTabStorage
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.PageSnapshotBuilder
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.RealBrowserTabPoolService
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.RealBrowserTabStorage
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.TabMemoryRestrictionService
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.PageSessionFactory

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
    fun provideTabMemoryRestrictionService(context: Context): TabMemoryRestrictionService {
        return TabMemoryRestrictionService(context)
    }

    @FeatureScope
    @Provides
    fun providePageSessionFactory(): PageSessionFactory {
        return PageSessionFactory()
    }

    @FeatureScope
    @Provides
    fun provideBrowserTabPoolService(
        browserTabStorage: BrowserTabStorage,
        pageSnapshotBuilder: PageSnapshotBuilder,
        tabMemoryRestrictionService: TabMemoryRestrictionService,
        pageSessionFactory: PageSessionFactory
    ): BrowserTabPoolService {
        return RealBrowserTabPoolService(
            browserTabStorage = browserTabStorage,
            pageSnapshotBuilder = pageSnapshotBuilder,
            tabMemoryRestrictionService = tabMemoryRestrictionService,
            pageSessionFactory = pageSessionFactory
        )
    }
}
