package io.novafoundation.nova.app.root.data.browser

import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.core_db.dao.BrowserTabsDao
import kotlinx.coroutines.flow.Flow

class TabsRepository(private val tabsDao: BrowserTabsDao) {

    fun observeTabIds(): Flow<List<String>> {
        return tabsDao.observeAllTabs().mapList { it.id }
    }

    suspend fun removeAllTabs() {
        tabsDao.removeAllTabs()
    }
}
