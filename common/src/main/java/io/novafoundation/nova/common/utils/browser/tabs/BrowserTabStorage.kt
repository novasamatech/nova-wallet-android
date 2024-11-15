package io.novafoundation.nova.common.utils.browser.tabs

import io.novafoundation.nova.common.utils.browser.tabs.models.BrowserTab
import io.novafoundation.nova.common.utils.browser.tabs.models.PageSnapshot
import kotlinx.coroutines.flow.Flow

interface BrowserTabStorage {

    suspend fun saveTab(tab: BrowserTab)

    suspend fun removeTab(tabId: String)

    suspend fun removeAllTabs()

    suspend fun savePageSnapshot(tabId: String, snapshot: PageSnapshot)

    fun observeTabs(): Flow<List<BrowserTab>>
}
