package io.novafoundation.nova.feature_dapp_impl.utils.tabs

import io.novafoundation.nova.common.utils.CallbackLruCache
import io.novafoundation.nova.common.utils.Urls
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_dapp_api.data.repository.DAppMetadataRepository
import io.novafoundation.nova.feature_dapp_impl.data.repository.tabs.BrowserTabInternalRepository
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.BrowserTab
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.CurrentTabState
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.BrowserTabSession
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.BrowserTabSessionFactory
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.OnPageChangedCallback
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.PageSnapshot
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.TabsState
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.fromName
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RealBrowserTabService(
    private val dAppMetadataRepository: DAppMetadataRepository,
    private val browserTabInternalRepository: BrowserTabInternalRepository,
    private val accountRepository: AccountRepository,
    private val pageSnapshotBuilder: PageSnapshotBuilder,
    private val tabMemoryRestrictionService: TabMemoryRestrictionService,
    private val browserTabSessionFactory: BrowserTabSessionFactory,
    private val rootScope: RootScope
) : BrowserTabService, CoroutineScope by rootScope, OnPageChangedCallback {

    private val availableSessionsCount = tabMemoryRestrictionService.getMaximumActiveSessions()

    private val selectedTabIdFlow = MutableStateFlow<String?>(null)

    private val allTabsFlow = accountRepository.selectedMetaAccountFlow()
        .flatMapLatest { browserTabInternalRepository.observeTabs(it.id) }
        .map { tabs -> tabs.associateBy { it.id } }

    private val activeSessions = CallbackLruCache<String, BrowserTabSession>(availableSessionsCount)

    override val tabStateFlow = combine(
        selectedTabIdFlow,
        accountRepository.selectedMetaAccountFlow(),
        allTabsFlow
    ) { selectedTabId, metaAccount, allTabs ->
        TabsState(
            tabs = allTabs.values.toList(),
            selectedTab = currentTabState(selectedTabId, allTabs)
        )
    }

    init {
        activeSessions.setOnEntryRemovedCallback {
            launch(Dispatchers.Main) {
                it.detachFromHost()
                it.destroy()
            }
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
            metaId = accountRepository.getSelectedMetaAccount().id,
            pageSnapshot = PageSnapshot.fromName(Urls.domainOf(url)),
            currentUrl = url,
            knownDAppMetadata = getKnownDappMetadata(url),
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
        activeSessions.remove(tabId)
    }

    override suspend fun removeAllTabs() {
        selectTab(null)
        browserTabInternalRepository.removeAllTabs()
        activeSessions.removeAll()
    }

    override fun makeCurrentTabSnapshot() {
        val currentTab = selectedTabIdFlow.value

        currentTab?.let { makeTabSnapshot(currentTab) }
    }

    private suspend fun addNewSession(tab: BrowserTab): BrowserTabSession {
        val session = browserTabSessionFactory.create(tabId = tab.id, startUrl = tab.currentUrl, onPageChangedCallback = this)
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

    /*
    We should update page title and url each time page is changed to have correct tab state in persistent storage
     */
    override fun onPageChanged(tabId: String, url: String?, title: String?) {
        if (url == null) return

        launch {
            withContext(Dispatchers.Default) {
                activeSessions[tabId].currentUrl = url
                browserTabInternalRepository.changeCurrentUrl(tabId, url)

                val metadata = getKnownDappMetadata(url)
                browserTabInternalRepository.changeKnownDAppMetadata(tabId, metadata?.iconLink)
            }
        }
    }

    private fun makeTabSnapshot(tabId: String) {
        val pageSession = activeSessions.get(tabId)

        if (pageSession != null) {
            val snapshot = pageSnapshotBuilder.getPageSnapshot(pageSession)

            launch(Dispatchers.Default) {
                browserTabInternalRepository.savePageSnapshot(pageSession.tabId, snapshot)
            }
        }
    }

    private suspend fun getKnownDappMetadata(url: String): BrowserTab.KnownDAppMetadata? {
        return dAppMetadataRepository.getDAppMetadata(Urls.normalizeUrl(url))?.let {
            BrowserTab.KnownDAppMetadata(it.iconLink)
        }
    }
}
