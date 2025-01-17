package io.novafoundation.nova.feature_dapp_impl.presentation.authorizedDApps

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_dapp_impl.presentation.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.domain.authorizedDApps.AuthorizedDApp
import io.novafoundation.nova.feature_dapp_impl.domain.authorizedDApps.AuthorizedDAppsInteractor
import io.novafoundation.nova.feature_dapp_impl.presentation.authorizedDApps.model.AuthorizedDAppModel
import kotlinx.coroutines.launch

typealias RevokeAuthorizationPayload = String // dApp name

class AuthorizedDAppsViewModel(
    private val interactor: AuthorizedDAppsInteractor,
    private val router: DAppRouter,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    walletUiUseCase: WalletUiUseCase,
) : BaseViewModel() {

    val revokeAuthorizationConfirmation = actionAwaitableMixinFactory.confirmingAction<RevokeAuthorizationPayload>()

    val walletUi = walletUiUseCase.selectedWalletUiFlow(showAddressIcon = true)
        .shareInBackground()

    val authorizedDApps = interactor.observeAuthorizedDApps()
        .mapList(::mapAuthorizedDAppToModel)
        .shareInBackground()

    fun backClicked() {
        router.back()
    }

    fun revokeClicked(item: AuthorizedDAppModel) = launch {
        val dAppTitle = item.title ?: item.url
        revokeAuthorizationConfirmation.awaitAction(dAppTitle)

        interactor.revokeAuthorization(item.url)
    }

    private fun mapAuthorizedDAppToModel(
        authorizedDApp: AuthorizedDApp
    ): AuthorizedDAppModel {
        return AuthorizedDAppModel(
            title = authorizedDApp.name,
            url = authorizedDApp.baseUrl,
            iconLink = authorizedDApp.iconLink
        )
    }
}
