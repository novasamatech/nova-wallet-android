package io.novafoundation.nova.feature_dapp_impl.utils.tabs

import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.BrowserTab
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.PageSnapshot
import kotlinx.coroutines.flow.Flow

interface BrowserTabStorage {

    suspend fun saveTab(tab: BrowserTab)

    suspend fun removeTab(tabId: String)

    suspend fun removeAllTabs()

    suspend fun savePageSnapshot(tabId: String, snapshot: PageSnapshot)

    fun observeTabs(): Flow<List<BrowserTab>>
}
