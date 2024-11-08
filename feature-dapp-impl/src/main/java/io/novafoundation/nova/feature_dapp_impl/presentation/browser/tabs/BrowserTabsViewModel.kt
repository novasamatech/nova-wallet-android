package io.novafoundation.nova.feature_dapp_impl.presentation.browser.tabs

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.tabPool.TabPoolService
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.tabPool.TabState

class BrowserTabsViewModel(
    private val tabPoolService: TabPoolService,
    private val router: DAppRouter
) : BaseViewModel() {

    val tabs = flowOf { tabPoolService.getAllTabs() }
        .mapList { TabStateModel(it.id, it.previewImagePath) }

    fun onTabClick(id: String) {
        val tab = tabPoolService.getAllTabs().first { it.id == id }
        selectTab(tab)
    }

    fun selectTab(tab: TabState) {
        tabPoolService.selectTab(tab)
        router.back()
    }
}
