package io.novafoundation.nova.feature_dapp_impl.presentation.browser

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.domain.browser.DappBrowserInteractor
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.PolkadotJsExtensionFactory
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.PolkadotJsExtensionRequest.AccountList
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.PolkadotJsExtensionRequest.AuthorizeTab
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class DAppBrowserViewModel(
    private val router: DAppRouter,
    private val polkadotJsExtensionFactory: PolkadotJsExtensionFactory,
    private val interactor: DappBrowserInteractor
) : BaseViewModel() {

    private val polkadotJsExtension = polkadotJsExtensionFactory.create(scope = this)

    init {
        polkadotJsExtension.requestsFlow
            .onEach {
                when (it) {
                    is AuthorizeTab -> authorizeTab(it)
                    is AccountList -> supplyAccountList(it)
                }
            }
            .inBackground()
            .launchIn(this)
    }

    private fun authorizeTab(request: AuthorizeTab) {
        request.accept(AuthorizeTab.Response(authorized = true))
    }

    private suspend fun supplyAccountList(request: AccountList) {
        val injectedAccounts = interactor.getInjectedAccounts()

        request.accept(AccountList.Response(injectedAccounts))
    }
}
