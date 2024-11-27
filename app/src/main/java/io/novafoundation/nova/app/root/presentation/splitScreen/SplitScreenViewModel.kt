package io.novafoundation.nova.app.root.presentation.splitScreen

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.domain.SplitScreenInteractor
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.awaitAction
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_dapp_api.presentation.browser.main.DAppBrowserPayload
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SplitScreenViewModel(
    private val interactor: SplitScreenInteractor,
    private val router: DAppRouter,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val resourceManager: ResourceManager
) : BaseViewModel() {

    val closeAllTabsConfirmation = actionAwaitableMixinFactory.confirmingAction<Unit>()

    private val tabsFlow = interactor.observeTabsWithNames()
        .shareInBackground()

    val tabsTitle = tabsFlow.map { tabs ->
        if (tabs.size == 1) {
            tabs.values.single() ?: tabSizeTitle(tabs.size)
        } else {
            tabSizeTitle(tabs.size)
        }
    }

    val dappTabsVisible = tabsFlow.map { it.isNotEmpty() }

    fun onTabsClicked() = launch {
        val tabIds = tabsFlow.first().keys

        if (tabIds.size == 1) {
            val payload = DAppBrowserPayload.Tab(tabIds.single())
            router.openDAppBrowser(payload)
        } else {
            router.openTabs()
        }
    }

    fun onTabsCloseClicked() = launch {
        closeAllTabsConfirmation.awaitAction()

        interactor.removeAllTabs()
    }

    private fun tabSizeTitle(size: Int) = resourceManager.getString(R.string.dapp_entry_point_title, size)
}
