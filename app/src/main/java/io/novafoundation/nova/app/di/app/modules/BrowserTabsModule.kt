package io.novafoundation.nova.app.di.app.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.App
import io.novafoundation.nova.app.root.data.browser.RealBrowserTabStorage
import io.novafoundation.nova.app.root.presentation.common.RealBuildTypeProvider
import io.novafoundation.nova.app.root.presentation.common.RootActivityIntentProvider
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.common.interfaces.ActivityIntentProvider
import io.novafoundation.nova.common.interfaces.BuildTypeProvider
import io.novafoundation.nova.common.interfaces.FileProvider
import io.novafoundation.nova.common.utils.browser.tabs.BrowserTabPoolService
import io.novafoundation.nova.common.utils.browser.tabs.BrowserTabStorage
import io.novafoundation.nova.common.utils.browser.tabs.PageSnapshotBuilder
import io.novafoundation.nova.common.utils.browser.tabs.RealBrowserTabPoolService
import io.novafoundation.nova.core_db.dao.BrowserTabsDao

@Module
class BrowserTabsModule {

    @ApplicationScope
    @Provides
    fun provideBrowserTabStorage(
        browserTabsDao: BrowserTabsDao,
    ): BrowserTabStorage {
        return RealBrowserTabStorage(browserTabsDao = browserTabsDao)
    }

    @ApplicationScope
    @Provides
    fun providePageSnapshotBuilder(fileProvider: FileProvider): PageSnapshotBuilder {
        return PageSnapshotBuilder(fileProvider)
    }

    @ApplicationScope
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
