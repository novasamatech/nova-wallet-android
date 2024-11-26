package io.novafoundation.nova.feature_dapp_impl.utils.tabs

import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.TabsState
import kotlinx.coroutines.flow.Flow

interface BrowserTabPoolService {

    val tabStateFlow: Flow<TabsState>

    fun selectTab(tabId: String?)

    fun detachCurrentSession()

    suspend fun makeCurrentTabSnapshot()

    suspend fun createNewTabAsCurrentTab(url: String)

    suspend fun removeTab(tabId: String)

    suspend fun removeAllTabs()
}
