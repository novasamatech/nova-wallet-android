package io.novafoundation.nova.feature_dapp_impl.presentation.tab

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.awaitAction
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.feature_dapp_api.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.BrowserTabPoolService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class BrowserTabsViewModel(
    private val router: DAppRouter,
    private val browserTabPoolService: BrowserTabPoolService,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
) : BaseViewModel() {

    val closeAllTabsConfirmation = actionAwaitableMixinFactory.confirmingAction<Unit>()

    val tabsFlow = browserTabPoolService.tabStateFlow
        .map { it.tabs }
        .mapList {
            BrowserTabRvItem(
                tabId = it.id,
                tabName = it.pageSnapshot.pageName,
                tabFaviconPath = it.pageSnapshot.pageIconPath,
                tabScreenshotPath = it.pageSnapshot.pagePicturePath
            )
        }

    fun openTab(tabId: String) = launch {
        val tab = browserTabPoolService.tabStateFlow.first().tabs.first { it.id == tabId }
        browserTabPoolService.selectTab(tabId)
        router.openDAppBrowser(tab.currentUrl)
    }

    fun closeTab(tabId: String) = launch {
        browserTabPoolService.removeTab(tabId)
    }

    fun closeAllTabs() = launch {
        closeAllTabsConfirmation.awaitAction()

        browserTabPoolService.removeAllTabs()
    }

    fun addTab() {
        router.openDappSearch()
    }

    fun done() {
        router.back()
    }
}
