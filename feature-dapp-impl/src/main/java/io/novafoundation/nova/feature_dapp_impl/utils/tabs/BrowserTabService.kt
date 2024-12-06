package io.novafoundation.nova.feature_dapp_impl.utils.tabs

import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.BrowserTab
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.TabsState
import kotlinx.coroutines.flow.Flow

interface BrowserTabService {

    val tabStateFlow: Flow<TabsState>

    fun selectTab(tabId: String?)

    fun detachCurrentSession()

    suspend fun makeCurrentTabSnapshot()

    suspend fun createNewTab(url: String): BrowserTab

    suspend fun removeTab(tabId: String)

    suspend fun removeAllTabs()
}

suspend fun BrowserTabService.createAndSelectTab(url: String) {
    val tab = createNewTab(url)
    selectTab(tab.id)
}
