package io.novafoundation.nova.feature_dapp_impl.utils.tabs

import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.BrowserTab
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.CurrentTabState
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.TabsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface BrowserTabService {

    val tabStateFlow: Flow<TabsState>

    fun selectTab(tabId: String?)

    fun detachCurrentSession()

    fun makeCurrentTabSnapshot()

    suspend fun createNewTab(url: String): BrowserTab

    suspend fun removeTab(tabId: String)

    suspend fun removeTabsForMetaAccount(metaId: Long)
}

suspend fun BrowserTabService.createAndSelectTab(url: String) {
    val tab = createNewTab(url)
    selectTab(tab.id)
}

suspend fun BrowserTabService.hasSelectedTab(): Boolean {
    return tabStateFlow.first().selectedTab is CurrentTabState.Selected
}
