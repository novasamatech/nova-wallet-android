package io.novafoundation.nova.app.root.presentation.splitScreen

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.domain.SplitScreenInteractor
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.awaitAction
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.common.navigation.DelayedNavigationRouter
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Consumer
import io.novafoundation.nova.feature_dapp_api.data.model.SimpleTabModel
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_dapp_api.presentation.browser.main.DAppBrowserPayload
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class TabsTitleModel(
    val title: String,
    val iconPath: String?
)

class SplitScreenViewModel(
    private val interactor: SplitScreenInteractor,
    private val router: DAppRouter,
    private val delayedNavigationRouter: DelayedNavigationRouter,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val resourceManager: ResourceManager,
    private val payload: SplitScreenPayload
) : BaseViewModel() {

    private val consumablePayload = Consumer(payload)

    val closeAllTabsConfirmation = actionAwaitableMixinFactory.confirmingAction<Unit>()

    private val tabsFlow = interactor.observeTabNamesById()
        .shareInBackground()

    val tabsTitle = tabsFlow.map { tabs ->
        if (tabs.size == 1) {
            singleTabTitle(tabs.single())
        } else {
            tabSizeTitle(tabs.size)
        }
    }.distinctUntilChanged()

    val dappTabsVisible = tabsFlow.map { it.isNotEmpty() }
        .distinctUntilChanged()

    fun onTabsClicked() = launch {
        val tabs = tabsFlow.first()

        if (tabs.size == 1) {
            val payload = DAppBrowserPayload.Tab(tabs.single().tabId)
            router.openDAppBrowser(payload)
        } else {
            router.openTabs()
        }
    }

    fun onTabsCloseClicked() = launch {
        closeAllTabsConfirmation.awaitAction()

        interactor.removeAllTabs()
    }

    private fun singleTabTitle(tab: SimpleTabModel): TabsTitleModel {
        return tab.title?.let {
            TabsTitleModel(it, tab.iconPath)
        } ?: tabSizeTitle(1)
    }

    private fun tabSizeTitle(size: Int): TabsTitleModel {
        return TabsTitleModel(
            resourceManager.getString(R.string.dapp_entry_point_title, size),
            null
        )
    }

    fun onNavigationAttached() {
        consumablePayload.useOnce {
            when (it) {
                is SplitScreenPayload.InstantNavigationOnAttach -> {
                    delayedNavigationRouter.runDelayedNavigation(it.delayedNavigation)
                }

                SplitScreenPayload.NoNavigation,
                null -> {
                } // Do nothing
            }
        }
    }
}
