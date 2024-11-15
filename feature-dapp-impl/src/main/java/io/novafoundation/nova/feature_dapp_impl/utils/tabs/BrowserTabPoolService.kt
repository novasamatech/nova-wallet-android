package io.novafoundation.nova.feature_dapp_impl.utils.tabs

import android.content.Context
import io.novafoundation.nova.common.utils.Urls
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.BrowserTab
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.CurrentTabState
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.PageSession
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.PageSnapshot
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.stateId
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.withNameOnly
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface BrowserTabPoolService {

    fun currentTabFlow(): Flow<CurrentTabState>

    fun selectTab(tabId: String)

    suspend fun makeCurrentTabSnapshot()

    suspend fun createNewTabAsCurrentTab(url: String)

    suspend fun removeTab(tabId: String)

    suspend fun removeAllTabs()
}

class RealBrowserTabPoolService(
    private val context: Context,
    private val browserTabStorage: BrowserTabStorage,
    private val pageSnapshotBuilder: PageSnapshotBuilder
) : BrowserTabPoolService {

    private val selectedTabIdFlow = MutableStateFlow<String?>(null)

    private val allTabsFlow = browserTabStorage.observeTabs()
        .map { tabs -> tabs.associateBy { it.id } }

    private val activeSessions = mutableMapOf<String, PageSession>()

    private val currentTabSession = combine(
        selectedTabIdFlow,
        allTabsFlow
    ) { selectedTabId, allTabs ->
        val tabId = selectedTabId ?: return@combine CurrentTabState.NotSelected
        val tab = allTabs[tabId] ?: return@combine CurrentTabState.NotSelected
        CurrentTabState.Selected(
            tab,
            activeSessions[tabId] ?: addNewSession(tab)
        )
    }.distinctUntilChangedBy { it.stateId() }

    override fun currentTabFlow(): Flow<CurrentTabState> {
        return currentTabSession
    }

    override fun selectTab(tabId: String) {
        selectedTabIdFlow.value = tabId
    }

    override suspend fun createNewTabAsCurrentTab(url: String) {
        val tab = BrowserTab(
            id = UUID.randomUUID().toString(),
            pageSnapshot = PageSnapshot.withNameOnly(Urls.domainOf(url)),
            currentUrl = url,
            creationTime = Date()
        )

        browserTabStorage.saveTab(tab)
        selectedTabIdFlow.value = tab.id
    }

    override suspend fun removeTab(tabId: String) {
        browserTabStorage.removeTab(tabId)
    }

    override suspend fun removeAllTabs() {
        browserTabStorage.removeAllTabs()
    }

    override suspend fun makeCurrentTabSnapshot() {
        val currentTab = currentTabSession.first()

        if (currentTab is CurrentTabState.Selected) {
            val snapshot = pageSnapshotBuilder.getPageSnapshot(currentTab.pageSession)
            browserTabStorage.savePageSnapshot(currentTab.tab.id, snapshot)
        }
    }

    private fun addNewSession(tab: BrowserTab): PageSession {
        val session = PageSession(tab.id, tab.currentUrl, context)
        activeSessions[tab.id] = session
        return session
    }
}
