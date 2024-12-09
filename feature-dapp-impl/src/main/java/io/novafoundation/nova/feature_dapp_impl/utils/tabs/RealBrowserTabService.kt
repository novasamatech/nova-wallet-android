package io.novafoundation.nova.feature_dapp_impl.utils.tabs

import io.novafoundation.nova.common.utils.CallbackLruCache
import io.novafoundation.nova.common.utils.Urls
import io.novafoundation.nova.feature_dapp_impl.data.repository.tabs.BrowserTabInternalRepository
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.BrowserTab
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.CurrentTabState
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.BrowserTabSession
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.BrowserTabSessionFactory
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.PageSnapshot
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.TabsState
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.fromName
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class RealBrowserTabService(
    private val browserTabInternalRepository: BrowserTabInternalRepository,
    private val pageSnapshotBuilder: PageSnapshotBuilder,
    private val tabMemoryRestrictionService: TabMemoryRestrictionService,
    private val browserTabSessionFactory: BrowserTabSessionFactory
) : BrowserTabService {

    private val availableSessionsCount = tabMemoryRestrictionService.getMaximumActiveSessions()

    private val selectedTabIdFlow = MutableStateFlow<String?>(null)

    private val allTabsFlow = browserTabInternalRepository.observeTabs()
        .map { tabs -> tabs.associateBy { it.id } }

    private val activeSessions = CallbackLruCache<String, BrowserTabSession>(availableSessionsCount)

    override val tabStateFlow = combine(
        selectedTabIdFlow,
        allTabsFlow
    ) { selectedTabId, allTabs ->
        TabsState(
            tabs = allTabs.values.toList(),
            selectedTab = currentTabState(selectedTabId, allTabs)
        )
    }

    init {
        activeSessions.setCallback {
            it.detachFromHost()
            it.destroy()
        }
    }

    override fun selectTab(tabId: String?) {
        val oldTabId = selectedTabIdFlow.value
        detachSession(oldTabId)

        selectedTabIdFlow.value = tabId
    }

    override fun detachCurrentSession() {
        selectTab(null)
    }

    override suspend fun createNewTab(url: String): BrowserTab {
        val tab = BrowserTab(
            id = UUID.randomUUID().toString(),
            pageSnapshot = PageSnapshot.fromName(Urls.domainOf(url)),
            currentUrl = url,
            creationTime = Date()
        )

        browserTabInternalRepository.saveTab(tab)

        return tab
    }

    override suspend fun removeTab(tabId: String) {
        if (tabId == selectedTabIdFlow.value) {
            selectTab(null)
        }

        browserTabInternalRepository.removeTab(tabId)
    }

    override suspend fun removeAllTabs() {
        selectTab(null)
        browserTabInternalRepository.removeAllTabs()
    }

    override suspend fun makeCurrentTabSnapshot() {
        val currentTab = selectedTabIdFlow.first()
        val pageSession = activeSessions.get(currentTab)

        if (pageSession != null) {
            val snapshot = pageSnapshotBuilder.getPageSnapshot(pageSession)
            browserTabInternalRepository.savePageSnapshot(pageSession.tabId, snapshot)
        }
    }

    private suspend fun addNewSession(tab: BrowserTab): BrowserTabSession {
        val session = browserTabSessionFactory.create(tabId = tab.id, startUrl = tab.currentUrl)
        activeSessions.put(tab.id, session)
        return session
    }

    private fun detachSession(tabId: String?) {
        if (tabId == null) return

        val sessionToDetach = activeSessions[tabId]
        sessionToDetach?.detachFromHost()
    }

    private suspend fun currentTabState(selectedTabId: String?, allTabs: Map<String, BrowserTab>): CurrentTabState {
        val tabId = selectedTabId ?: return CurrentTabState.NotSelected
        val tab = allTabs[tabId] ?: return CurrentTabState.NotSelected
        return CurrentTabState.Selected(
            tab,
            activeSessions[tabId] ?: addNewSession(tab)
        )
    }
}
