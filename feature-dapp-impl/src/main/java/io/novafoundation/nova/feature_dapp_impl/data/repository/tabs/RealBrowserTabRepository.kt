package io.novafoundation.nova.feature_dapp_impl.data.repository.tabs

import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.core_db.dao.BrowserTabsDao
import io.novafoundation.nova.core_db.model.BrowserTabLocal
import io.novafoundation.nova.feature_dapp_api.data.model.SimpleTabModel
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.BrowserTab
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.PageSnapshot
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class RealBrowserTabRepository(
    private val browserTabsDao: BrowserTabsDao
) : BrowserTabInternalRepository {

    override suspend fun saveTab(tab: BrowserTab) {
        browserTabsDao.insertTab(tab.toLocal())
    }

    override suspend fun removeTab(tabId: String) {
        browserTabsDao.removeTab(tabId)
    }

    override fun observeTabsWithNames(metaId: Long): Flow<List<SimpleTabModel>> {
        return browserTabsDao.observeTabsByMetaId(metaId)
            .mapList {
                SimpleTabModel(it.id, it.pageName, it.knownDAppMetadata?.iconLink, it.pageIconPath)
            }
    }

    override suspend fun removeTabsForMetaAccount(metaId: Long): List<String> {
        return withContext(Dispatchers.Default) {
            browserTabsDao.removeTabsByMetaId(metaId)
        }
    }

    override suspend fun savePageSnapshot(tabId: String, snapshot: PageSnapshot) {
        browserTabsDao.updatePageSnapshot(
            tabId = tabId,
            pageName = snapshot.pageName,
            pageIconPath = snapshot.pageIconPath,
            pagePicturePath = snapshot.pagePicturePath
        )
    }

    override fun observeTabs(metaId: Long): Flow<List<BrowserTab>> {
        return browserTabsDao.observeTabsByMetaId(metaId).mapList { tab ->
            tab.fromLocal()
        }
    }

    override suspend fun changeCurrentUrl(tabId: String, url: String) {
        withContext(Dispatchers.Default) { browserTabsDao.updateCurrentUrl(tabId, url) }
    }

    override suspend fun changeKnownDAppMetadata(tabId: String, dappIconUrl: String?) {
        withContext(Dispatchers.Default) { browserTabsDao.updateKnownDAppMetadata(tabId, dappIconUrl) }
    }
}

private fun BrowserTabLocal.fromLocal(): BrowserTab {
    return BrowserTab(
        id = id,
        metaId = metaId,
        currentUrl = currentUrl,
        pageSnapshot = PageSnapshot(
            pageName = pageName,
            pageIconPath = pageIconPath,
            pagePicturePath = pagePicturePath
        ),
        knownDAppMetadata = knownDAppMetadata?.let { BrowserTab.KnownDAppMetadata(it.iconLink) },
        creationTime = Date(creationTime),
    )
}

private fun BrowserTab.toLocal(): BrowserTabLocal {
    return BrowserTabLocal(
        id = id,
        metaId = metaId,
        currentUrl = currentUrl,
        creationTime = creationTime.time,
        pageName = pageSnapshot.pageName,
        pageIconPath = pageSnapshot.pageIconPath,
        knownDAppMetadata = knownDAppMetadata?.let { BrowserTabLocal.KnownDAppMetadata(it.iconLink) },
        pagePicturePath = pageSnapshot.pagePicturePath
    )
}
