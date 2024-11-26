package io.novafoundation.nova.app.root.presentation.splitScreen

import io.novafoundation.nova.app.root.domain.SplitScreenInteractor
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.awaitAction
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.feature_dapp_api.DAppRouter
import io.novafoundation.nova.feature_dapp_api.presentation.browser.main.DAppBrowserPayload
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SplitScreenViewModel(
    private val interactor: SplitScreenInteractor,
    private val router: DAppRouter,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
) : BaseViewModel() {

    val closeAllTabsConfirmation = actionAwaitableMixinFactory.confirmingAction<Unit>()

    private val tabIdsFlow = interactor.observeTabIds()
        .shareInBackground()

    val dappTabsQuantity = tabIdsFlow.map { it.size }

    fun onTabsClicked() = launch {
        val tabIds = tabIdsFlow.first()

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
}
