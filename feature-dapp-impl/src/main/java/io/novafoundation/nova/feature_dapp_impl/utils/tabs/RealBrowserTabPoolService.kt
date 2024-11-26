package io.novafoundation.nova.feature_dapp_impl.utils.tabs

import io.novafoundation.nova.common.utils.Urls
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.BrowserTab
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.CurrentTabState
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.PageSession
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.PageSessionFactory
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.PageSnapshot
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.TabsState
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.withNameOnly
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class RealBrowserTabPoolService(
    private val browserTabStorage: BrowserTabStorage,
    private val pageSnapshotBuilder: PageSnapshotBuilder,
    private val tabMemoryRestrictionService: TabMemoryRestrictionService,
    private val pageSessionFactory: PageSessionFactory
) : BrowserTabPoolService {

    private val availableSessionsCount = tabMemoryRestrictionService.getMaximumActiveSessions()

    private val selectedTabIdFlow = MutableStateFlow<String?>(null)

    private val allTabsFlow = browserTabStorage.observeTabs()
        .map { tabs -> tabs.associateBy { it.id } }

    private val activeSessions = mutableMapOf<String, PageSession>()

    override val tabStateFlow = combine(
        selectedTabIdFlow,
        allTabsFlow
    ) { selectedTabId, allTabs ->
        TabsState(
            tabs = allTabs.values.toList(),
            selectedTab = currentTabState(selectedTabId, allTabs)
        )
    }

    override fun selectTab(tabId: String?) {
        val oldTabId = selectedTabIdFlow.value
        detachSession(oldTabId)

        selectedTabIdFlow.value = tabId
    }

    override fun detachCurrentSession() {
        selectTab(null)
    }

    override suspend fun createNewTabAsCurrentTab(url: String) {
        val tab = BrowserTab(
            id = UUID.randomUUID().toString(),
            pageSnapshot = PageSnapshot.withNameOnly(Urls.domainOf(url)),
            currentUrl = url,
            creationTime = Date()
        )

        browserTabStorage.saveTab(tab)
        selectTab(tab.id)
    }

    override suspend fun removeTab(tabId: String) {
        if (tabId == selectedTabIdFlow.value) {
            selectTab(null)
        }

        browserTabStorage.removeTab(tabId)
    }

    override suspend fun removeAllTabs() {
        selectTab(null)
        browserTabStorage.removeAllTabs()
    }

    override suspend fun makeCurrentTabSnapshot() {
        val currentTab = selectedTabIdFlow.first()
        val pageSession = activeSessions[currentTab]

        if (pageSession != null) {
            val snapshot = pageSnapshotBuilder.getPageSnapshot(pageSession)
            browserTabStorage.savePageSnapshot(pageSession.tabId, snapshot)
        }
    }

    private fun addNewSession(tab: BrowserTab): PageSession {
        if (activeSessions.size >= availableSessionsCount) {
            removeOldestSession()
        }

        val session = pageSessionFactory.create(tabId = tab.id, sessionStartTime = Date(), startUrl = tab.currentUrl)
        activeSessions[tab.id] = session
        return session
    }

    private fun removeOldestSession() {
        val sessionToDestroy = activeSessions.values.minBy { it.sessionStartTime.time }
        sessionToDestroy.destroySession()
        activeSessions.remove(sessionToDestroy.tabId)
    }

    private fun detachSession(tabId: String?) {
        val sessionToDetach = activeSessions[tabId]
        sessionToDetach?.detachSession()
    }

    private fun currentTabState(selectedTabId: String?, allTabs: Map<String, BrowserTab>): CurrentTabState {
        val tabId = selectedTabId ?: return CurrentTabState.NotSelected
        val tab = allTabs[tabId] ?: return CurrentTabState.NotSelected
        return CurrentTabState.Selected(
            tab,
            activeSessions[tabId] ?: addNewSession(tab)
        )
    }
}
