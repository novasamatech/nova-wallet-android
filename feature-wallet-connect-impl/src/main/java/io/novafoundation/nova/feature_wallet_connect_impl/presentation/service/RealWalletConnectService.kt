package io.novafoundation.nova.feature_wallet_connect_impl.presentation.service

import android.util.Log
import com.walletconnect.android.Core
import com.walletconnect.android.CoreClient
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Web3Wallet
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingOrDenyingAction
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignRequester
import io.novafoundation.nova.feature_external_sign_api.model.awaitConfirmation
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.ExternalSignPayload
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.ExternalSignWallet
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.SigningDappMetadata
import io.novafoundation.nova.feature_external_sign_api.presentation.externalSign.AuthorizeDappBottomSheet
import io.novafoundation.nova.feature_wallet_connect_api.presentation.WalletConnectService
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.WalletConnectSessionInteractor
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.WalletConnectSessionsEvent
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.sessionEventsFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

internal class RealWalletConnectServiceFactory(
    private val awaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val walletUiUseCase: WalletUiUseCase,
    private val interactor: WalletConnectSessionInteractor,
    private val dAppSignRequester: ExternalSignRequester,
    private val selectedAccountUseCase: SelectedAccountUseCase,
) : WalletConnectService.Factory {

    override fun create(coroutineScope: CoroutineScope): WalletConnectService {
        return RealWalletConnectService(
            parentScope = coroutineScope,
            awaitableMixinFactory = awaitableMixinFactory,
            walletUiUseCase = walletUiUseCase,
            interactor = interactor,
            dAppSignRequester = dAppSignRequester,
            selectedAccountUseCase = selectedAccountUseCase
        )
    }
}

internal class RealWalletConnectService(
    parentScope: CoroutineScope,
    private val awaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val walletUiUseCase: WalletUiUseCase,
    private val interactor: WalletConnectSessionInteractor,
    private val dAppSignRequester: ExternalSignRequester,
    private val selectedAccountUseCase: SelectedAccountUseCase,
) : WalletConnectService,
    CoroutineScope by parentScope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(parentScope) {

    override val authorizeDapp = awaitableMixinFactory.confirmingOrDenyingAction<AuthorizeDappBottomSheet.Payload>()

    private val selectedWalletUiFlow = walletUiUseCase.selectedWalletUiFlow(showAddressIcon = true)
        .shareInBackground()

    private val events = Web3Wallet.sessionEventsFlow(scope = this)

    init {
        events.onEach {
            when (it) {
                is WalletConnectSessionsEvent.SessionProposal -> handleSessionProposal(it.proposal)
                is WalletConnectSessionsEvent.SessionRequest -> handleSessionRequest(it.request)
                is WalletConnectSessionsEvent.SessionSettlement -> handleSessionSettlement(it.settlement)
                is WalletConnectSessionsEvent.SessionDeleted -> handleSessionDelete(it.delete)
            }
        }
            .inBackground()
            .launchIn(this)
    }

    override fun connect() {
        CoreClient.Relay.connect { error: Core.Model.Error ->
            Log.d(LOG_TAG, "Failed to connect to Wallet Connect: ", error.throwable)
        }
    }

    override fun disconnect() {
        CoreClient.Relay.disconnect { error: Core.Model.Error ->
            Log.d(LOG_TAG, "Failed to disconnect to Wallet Connect: ", error.throwable)
        }
    }

    private suspend fun handleSessionProposal(proposal: Wallet.Model.SessionProposal) {
        // TODO allow to switch meta account
        val metaAccount = selectedAccountUseCase.getSelectedMetaAccount()

        val payload = AuthorizeDappBottomSheet.Payload(
            title = proposal.name,
            dAppIconUrl = proposal.icons.firstOrNull()?.toString(),
            dAppUrl = proposal.url,
            walletModel = selectedWalletUiFlow.first()
        )

        val allowed = authorizeDapp.awaitAction(payload)

        if (allowed) {
            interactor.approveSession(proposal, metaAccount)
                .onFailure {
                    Log.d("WalletConnect", "Session approve failed", it)
                }
        } else {
            interactor.rejectSession(proposal)
        }
    }

    private suspend fun handleSessionRequest(sessionRequest: Wallet.Model.SessionRequest) {
        val sdkSession = Web3Wallet.getActiveSessionByTopic(sessionRequest.topic) ?: return
        val appSession = interactor.getSession(sessionRequest.topic) ?: return

        // TODO reject request if not able to parse
        val walletConnectRequest = interactor.parseSessionRequest(sessionRequest)
            .onFailure { Log.e("WalletConnect", "Failed to parse session request $sessionRequest", it) }
            .getOrNull() ?: return

        val externalSignResponse = withContext(Dispatchers.Main) {
            dAppSignRequester.awaitConfirmation(
                ExternalSignPayload(
                    signRequest = walletConnectRequest.toExternalSignRequest(),
                    dappMetadata = mapWalletConnectSessionToSignDAppMetadata(sdkSession),
                    wallet = ExternalSignWallet.WithId(appSession.metaId)
                )
            )
        }

        walletConnectRequest.respondWith(externalSignResponse)
    }

    private suspend fun handleSessionSettlement(settlement: Wallet.Model.SettledSessionResponse) {
        interactor.onSessionSettled(settlement)
    }

    private suspend fun handleSessionDelete(settlement: Wallet.Model.SessionDelete) {
        interactor.onSessionDelete(settlement)
    }

    private fun mapWalletConnectSessionToSignDAppMetadata(session: Wallet.Model.Session): SigningDappMetadata? {
        return session.metaData?.run {
            SigningDappMetadata(
                icon = icons.firstOrNull(),
                name = name,
                url = url
            )
        }
    }
}
