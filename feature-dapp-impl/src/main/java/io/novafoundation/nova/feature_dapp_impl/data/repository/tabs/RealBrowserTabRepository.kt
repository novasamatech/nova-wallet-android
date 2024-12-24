package io.novafoundation.nova.feature_dapp_impl.data.repository.tabs

import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.core_db.dao.BrowserTabsDao
import io.novafoundation.nova.core_db.model.BrowserTabLocal
import io.novafoundation.nova.feature_dapp_api.data.model.SimpleTabModel
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.BrowserTab
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.PageSnapshot
import java.util.Date
import kotlinx.coroutines.flow.Flow

class RealBrowserTabRepository(
    private val browserTabsDao: BrowserTabsDao
) : BrowserTabInternalRepository {

    override suspend fun saveTab(tab: BrowserTab) {
        browserTabsDao.insertTab(tab.toLocal())
    }

    override suspend fun removeTab(tabId: String) {
        browserTabsDao.removeTab(tabId)
    }

    override fun observeTabsWithNames(): Flow<List<SimpleTabModel>> {
        return browserTabsDao.observeAllTabs()
            .mapList {
                SimpleTabModel(it.id, it.pageName, it.pageIconPath)
            }
    }

    override suspend fun removeAllTabs() {
        browserTabsDao.removeAllTabs()
    }

    override suspend fun savePageSnapshot(tabId: String, snapshot: PageSnapshot) {
        browserTabsDao.updatePageSnapshot(
            tabId = tabId,
            pageName = snapshot.pageName,
            pageIconPath = snapshot.pageIconPath,
            pagePicturePath = snapshot.pagePicturePath
        )
    }

    override fun observeTabs(): Flow<List<BrowserTab>> {
        return browserTabsDao.observeAllTabs().mapList { tab ->
            tab.fromLocal()
        }
    }

    override suspend fun changeCurrentUrl(tabId: String, url: String) {
        browserTabsDao.updateCurrentUrl(tabId, url)
    }
}

private fun BrowserTabLocal.fromLocal(): BrowserTab {
    return BrowserTab(
        id = id,
        currentUrl = currentUrl,
        pageSnapshot = PageSnapshot(
            pageName = pageName,
            pageIconPath = pageIconPath,
            pagePicturePath = pagePicturePath
        ),
        creationTime = Date(creationTime),
    )
}

private fun BrowserTab.toLocal(): BrowserTabLocal {
    return BrowserTabLocal(
        id = id,
        currentUrl = currentUrl,
        creationTime = creationTime.time,
        pageName = pageSnapshot.pageName,
        pageIconPath = pageSnapshot.pageIconPath,
        pagePicturePath = pageSnapshot.pagePicturePath
    )
}
