package io.novafoundation.nova.feature_dapp_impl.presentation.tab

import androidx.navigation.fragment.FragmentNavigator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.awaitAction
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.common.utils.Urls
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_dapp_api.presentation.browser.main.DAppBrowserPayload
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.BrowserTabService
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class BrowserTabsViewModel(
    private val router: DAppRouter,
    private val browserTabService: BrowserTabService,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val accountUseCase: SelectedAccountUseCase
) : BaseViewModel() {

    val closeAllTabsConfirmation = actionAwaitableMixinFactory.confirmingAction<Unit>()

    val tabsFlow = browserTabService.tabStateFlow
        .map { it.tabs }
        .mapList {
            BrowserTabRvItem(
                tabId = it.id,
                tabName = it.pageSnapshot.pageName ?: Urls.domainOf(it.currentUrl),
                tabFaviconPath = it.pageSnapshot.pageIconPath,
                tabScreenshotPath = it.pageSnapshot.pagePicturePath
            )
        }.shareInBackground()

    fun openTab(tab: BrowserTabRvItem, extras: FragmentNavigator.Extras) = launch {
        router.openDAppBrowser(DAppBrowserPayload.Tab(tab.tabId), extras)
    }

    fun closeTab(tabId: String) = launch {
        browserTabService.removeTab(tabId)
    }

    fun closeAllTabs() = launch {
        closeAllTabsConfirmation.awaitAction()

        val metaAccount = accountUseCase.getSelectedMetaAccount()
        browserTabService.removeTabsForMetaAccount(metaAccount.id)
        router.closeTabsScreen()
    }

    fun addTab() {
        router.openDappSearch()
    }

    fun done() {
        router.closeTabsScreen()
    }
}
