package io.novafoundation.nova.feature_dapp_impl.web3.states

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_dapp_impl.domain.browser.BrowserPageAnalyzed
import io.novafoundation.nova.feature_dapp_impl.web3.Web3Transport
import io.novafoundation.nova.feature_dapp_impl.web3.session.Web3Session
import io.novafoundation.nova.feature_dapp_impl.web3.states.hostApi.ConfirmTxResponse
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.ExternalSignRequest
import io.novafoundation.nova.feature_external_sign_api.presentation.externalSign.AuthorizeDappBottomSheet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

interface Web3ExtensionStateMachine<S> {

    val state: StateFlow<S>

    suspend fun transition(transition: suspend (StateMachineTransition<S>.(state: S) -> Unit))

    interface State<R : Web3Transport.Request<*>, S> {

        suspend fun acceptRequest(request: R, transition: StateMachineTransition<S>)

        suspend fun acceptEvent(event: ExternalEvent, transition: StateMachineTransition<S>)
    }

    sealed class ExternalEvent {

        object PhishingDetected : ExternalEvent()
    }

    interface StateMachineTransition<S> {

        fun emitState(newState: S)

        suspend fun <R : Web3Transport.Request<*>> State<R, S>.acceptRequest(request: R) = acceptRequest(request, this@StateMachineTransition)
        suspend fun State<*, S>.acceptEvent(event: ExternalEvent) = acceptEvent(event, this@StateMachineTransition)
    }
}

interface Web3StateMachineHost {

    object NotAuthorizedException : Exception("Rejected by user")
    object SigningFailedException : Exception("Signing failed")

    object SendHandledBySigner : Exception("Sending extrinsic was handled by Nova Wallet directly")

    val selectedAccount: Flow<MetaAccount>
    val currentPageAnalyzed: Flow<BrowserPageAnalyzed>

    val externalEvents: Flow<Web3ExtensionStateMachine.ExternalEvent>

    suspend fun authorizeDApp(payload: AuthorizeDappBottomSheet.Payload): Web3Session.Authorization.State
    suspend fun confirmTx(request: ExternalSignRequest): ConfirmTxResponse

    fun showError(text: String)

    fun showError(title: String, message: CharSequence)

    fun reloadPage()
}

suspend fun Web3StateMachineHost.selectedMetaAccountId() = selectedAccount.first().id
