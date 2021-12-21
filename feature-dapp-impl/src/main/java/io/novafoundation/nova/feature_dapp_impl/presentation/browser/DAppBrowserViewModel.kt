package io.novafoundation.nova.feature_dapp_impl.presentation.browser

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.PolkadotJsExtensionFactory
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.PolkadotJsExtensionRequest.AuthorizeTab
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class DAppBrowserViewModel(
    private val router: DAppRouter,
    private val polkadotJsExtensionFactory: PolkadotJsExtensionFactory,
) : BaseViewModel() {

    private val polkadotJsExtension = polkadotJsExtensionFactory.create(scope = this)

    init {
        polkadotJsExtension.requestsFlow
            .onEach {
                when (it) {
                    is AuthorizeTab -> {
                        it.accept(AuthorizeTab.Response(authorized = true))
                    }
                }
            }.launchIn(this)
    }
}
