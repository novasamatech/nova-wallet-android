package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_account_api.presenatation.chain.formatChainListOverview
import io.novafoundation.nova.feature_wallet_connect_impl.WalletConnectRouter
import io.novafoundation.nova.feature_wallet_connect_impl.domain.model.WalletConnectSessionDetails
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.WalletConnectSessionInteractor
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.common.WalletConnectSessionMapper
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.details.model.WalletConnectSessionDetailsUi
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
) : BaseViewModel() {

    private val _showChainBottomSheet = MutableLiveData<Event<List<ChainUi>>>()
    val showChainBottomSheet: LiveData<Event<List<ChainUi>>> = _showChainBottomSheet

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
        sessionFlow.first()?.sessionTopic?.let { sessionTopic ->
            interactor.disconnect(sessionTopic)
        }
    }

    private fun watchSessionDisconnect() {
        sessionFlow
            .distinctUntilChanged()
            .onEach { if (it == null) exit() }
            .launchIn(this)
    }

    private suspend fun mapSessionDetailsToUi(session: WalletConnectSessionDetails): WalletConnectSessionDetailsUi {
        val chainUis = session.chains.map(::mapChainToUi)

        return WalletConnectSessionDetailsUi(
            dappTitle = walletConnectSessionMapper.formatSessionDAppTitle(session.dappMetadata),
            dappUrl = session.dappMetadata?.dappUrl,
            dappIcon = session.dappMetadata?.icon,
            networksOverview = resourceManager.formatChainListOverview(chainUis),
            networks = chainUis,
            wallet = walletUiUseCase.walletUiFor(session.connectedMetaAccount)
        )
    }
}
