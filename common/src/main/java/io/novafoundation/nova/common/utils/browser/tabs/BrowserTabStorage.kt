package io.novafoundation.nova.common.utils.browser.tabs

import kotlinx.coroutines.flow.Flow

interface BrowserTabStorage {

    suspend fun saveTab(tab: BrowserTab)

    suspend fun removeTab(tabId: String)

    suspend fun removeAllTabs()

    suspend fun savePageSnapshot(snapshot: PageSnapshot)

    fun observeTabs(): Flow<List<BrowserTab>>
}
