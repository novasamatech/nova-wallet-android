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
    fun provideBrowserTabPoolService(
        context: Context,
        browserTabStorage: BrowserTabStorage,
        pageSnapshotBuilder: PageSnapshotBuilder,
        tabMemoryRestrictionService: TabMemoryRestrictionService
    ): BrowserTabPoolService {
        return RealBrowserTabPoolService(
            context = context,
            browserTabStorage = browserTabStorage,
            pageSnapshotBuilder = pageSnapshotBuilder,
            tabMemoryRestrictionService = tabMemoryRestrictionService
        )
    }
}
