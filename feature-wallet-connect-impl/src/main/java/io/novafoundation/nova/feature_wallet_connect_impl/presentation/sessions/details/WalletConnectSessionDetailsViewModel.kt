package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.withFlagSet
import io.novafoundation.nova.common.view.TableCellView.FieldStyle
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_account_api.presenatation.chain.formatChainListOverview
import io.novafoundation.nova.feature_wallet_connect_api.domain.sessions.WalletConnectSessionsUseCase
import io.novafoundation.nova.feature_wallet_connect_impl.R
import io.novafoundation.nova.feature_wallet_connect_impl.WalletConnectRouter
import io.novafoundation.nova.feature_wallet_connect_impl.domain.model.WalletConnectSessionDetails
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.WalletConnectSessionInteractor
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.common.WalletConnectSessionMapper
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.details.model.WalletConnectSessionDetailsUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class WalletConnectSessionDetailsViewModel(
    private val router: WalletConnectRouter,
    private val interactor: WalletConnectSessionInteractor,
    private val resourceManager: ResourceManager,
    private val walletUiUseCase: WalletUiUseCase,
    private val walletConnectSessionMapper: WalletConnectSessionMapper,
    private val payload: WalletConnectSessionDetailsPayload,
    private val walletConnectSessionsUseCase: WalletConnectSessionsUseCase,
) : BaseViewModel() {

    private val _showChainBottomSheet = MutableLiveData<Event<List<ChainUi>>>()
    val showChainBottomSheet: LiveData<Event<List<ChainUi>>> = _showChainBottomSheet

    private val disconnectInProgressFlow = MutableStateFlow(false)

    val disconnectButtonState = disconnectInProgressFlow.map { disconnectInProgress ->
        if (disconnectInProgress) {
            DescriptiveButtonState.Loading
        } else {
            DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_disconnect))
        }
    }.shareInBackground()

    private val sessionFlow = interactor.activeSessionFlow(payload.sessionTopic)
        .shareInBackground()

    val sessionUi = sessionFlow
        .filterNotNull()
        .map(::mapSessionDetailsToUi)
        .shareInBackground()

    init {
        watchSessionDisconnect()
    }

    fun exit() {
        router.back()
    }

    fun networksClicked() = launch {
        _showChainBottomSheet.value = sessionUi.first().networks.event()
    }

    fun disconnect() = launch {
        val sessionTopic = sessionFlow.first()?.sessionTopic ?: return@launch

        disconnectInProgressFlow.withFlagSet {
            interactor.disconnect(sessionTopic)
                .onFailure(::showError)
        }
    }

    private fun watchSessionDisconnect() {
        sessionFlow
            .distinctUntilChanged()
            .onEach { if (it == null) closeSessionsScreen() }
            .launchIn(this)
    }

    private suspend fun closeSessionsScreen() {
        val numberOfActiveSessions = walletConnectSessionsUseCase.activeSessionsNumber()

        if (numberOfActiveSessions > 0) {
            router.back()
        } else {
            router.backToSettings()
        }
    }

    private suspend fun mapSessionDetailsToUi(session: WalletConnectSessionDetails): WalletConnectSessionDetailsUi {
        val chainUis = session.chains.map(::mapChainToUi)

        return WalletConnectSessionDetailsUi(
            dappTitle = walletConnectSessionMapper.formatSessionDAppTitle(session.dappMetadata),
            dappUrl = session.dappMetadata?.dAppUrl,
            dappIcon = session.dappMetadata?.icon,
            networksOverview = resourceManager.formatChainListOverview(chainUis),
            networks = chainUis,
            wallet = walletUiUseCase.walletUiFor(session.connectedMetaAccount),
            status = mapSessionStatusToUi(session.status)
        )
    }

    private fun mapSessionStatusToUi(status: WalletConnectSessionDetails.SessionStatus): WalletConnectSessionDetailsUi.SessionStatus {
        return when (status) {
            WalletConnectSessionDetails.SessionStatus.ACTIVE -> WalletConnectSessionDetailsUi.SessionStatus(
                label = resourceManager.getString(R.string.common_active),
                labelStyle = FieldStyle.POSITIVE,
                icon = R.drawable.ic_indicator_positive_pulse
            )
            WalletConnectSessionDetails.SessionStatus.EXPIRED -> WalletConnectSessionDetailsUi.SessionStatus(
                label = resourceManager.getString(R.string.common_expired),
                labelStyle = FieldStyle.SECONDARY,
                icon = R.drawable.ic_indicator_inactive_pulse
            )
        }
    }
}
