package io.novafoundation.nova.feature_dapp_impl.presentation.tab

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.awaitAction
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_dapp_api.presentation.browser.main.DAppBrowserPayload
import io.novafoundation.nova.feature_dapp_impl.presentation.search.DAppSearchRequester
import io.novafoundation.nova.feature_dapp_impl.presentation.search.SearchPayload
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.BrowserTabService
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class BrowserTabsViewModel(
    private val router: DAppRouter,
    private val browserTabService: BrowserTabService,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val dAppSearchRequester: DAppSearchRequester,
) : BaseViewModel() {

    val closeAllTabsConfirmation = actionAwaitableMixinFactory.confirmingAction<Unit>()

    val tabsFlow = browserTabService.tabStateFlow
        .map { it.tabs }
        .mapList {
            BrowserTabRvItem(
                tabId = it.id,
                tabName = it.pageSnapshot.pageName,
                tabFaviconPath = it.pageSnapshot.pageIconPath,
                tabScreenshotPath = it.pageSnapshot.pagePicturePath
            )
        }.shareInBackground()

    fun openTab(tabId: String) = launch {
        router.openDAppBrowser(DAppBrowserPayload.Tab(tabId))
    }

    fun closeTab(tabId: String) = launch {
        browserTabService.removeTab(tabId)
    }

    fun closeAllTabs() = launch {
        closeAllTabsConfirmation.awaitAction()

        browserTabService.removeAllTabs()
        router.closeTabsScreen()
    }

    fun addTab() {
        dAppSearchRequester.openRequest(SearchPayload(initialUrl = null, SearchPayload.Request.CREATE_NEW_TAB))
    }

    fun done() {
        router.closeTabsScreen()
    }
}
