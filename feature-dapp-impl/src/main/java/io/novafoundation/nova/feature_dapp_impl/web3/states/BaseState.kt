package io.novafoundation.nova.feature_dapp_impl.web3.states

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor
import io.novafoundation.nova.feature_dapp_impl.web3.Web3Transport
import io.novafoundation.nova.feature_dapp_impl.web3.session.Web3Session
import io.novafoundation.nova.feature_external_sign_api.presentation.externalSign.AuthorizeDappBottomSheet
import kotlinx.coroutines.flow.first

abstract class BaseState<R : Web3Transport.Request<*>, S>(
    protected val commonInteractor: DappInteractor,
    protected val resourceManager: ResourceManager,
    protected val addressIconGenerator: AddressIconGenerator,
    protected val web3Session: Web3Session,
    protected val hostApi: Web3StateMachineHost,
    private val walletUiUseCase: WalletUiUseCase,
) : Web3ExtensionStateMachine.State<R, S> {

    suspend fun respondIfAllowed(
        ifAllowed: suspend () -> Unit,
        ifDenied: suspend () -> Unit
    ) = if (getAuthorizationStateForCurrentPage() == Web3Session.Authorization.State.ALLOWED) {
        ifAllowed()
    } else {
        ifDenied()
    }

    protected suspend fun authorizeDapp(): Boolean {
        return when (getAuthorizationStateForCurrentPage()) {
            // user already accepted - no need to ask second time
            Web3Session.Authorization.State.ALLOWED -> true
            // first time dapp request authorization during this session
            Web3Session.Authorization.State.NONE -> authorizePageWithConfirmation()
            // user rejected this dapp previosuly - ask for authorization one more time
            Web3Session.Authorization.State.REJECTED -> authorizePageWithConfirmation()
        }
    }

    protected suspend fun getAuthorizationStateForCurrentPage(): Web3Session.Authorization.State {
        return web3Session.authorizationStateFor(hostApi.currentPageAnalyzed.first().url, hostApi.selectedMetaAccountId())
    }

    /**
     * @return whether authorization request was approved or not
     */
    private suspend fun authorizePageWithConfirmation(): Boolean {
        val currentPage = hostApi.currentPageAnalyzed.first()
        val selectedAccount = hostApi.selectedAccount.first()
        // use url got from browser instead of url got from dApp to prevent dApp supplying wrong URL
        val dAppInfo = commonInteractor.getDAppInfo(dAppUrl = currentPage.url)

        val dAppIdentifier = dAppInfo.metadata?.name ?: dAppInfo.baseUrl

        val action = AuthorizeDappBottomSheet.Payload(
            title = resourceManager.getString(
                io.novafoundation.nova.feature_dapp_impl.R.string.dapp_confirm_authorize_title_format,
                dAppIdentifier
            ),
            dAppIconUrl = dAppInfo.metadata?.iconLink,
            dAppUrl = dAppInfo.baseUrl,
            walletModel = walletUiUseCase.selectedWalletUi()
        )

        val authorizationState = hostApi.authorizeDApp(action)

        web3Session.updateAuthorization(
            state = authorizationState,
            fullUrl = currentPage.url,
            dAppTitle = dAppIdentifier,
            metaId = selectedAccount.id
        )

        return authorizationState == Web3Session.Authorization.State.ALLOWED
    }
}
