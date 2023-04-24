package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.list

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_wallet_connect_impl.WalletConnectRouter
import io.novafoundation.nova.feature_wallet_connect_impl.domain.model.WalletConnectSession
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.WalletConnectSessionInteractor
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.common.WalletConnectSessionMapper
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.details.WalletConnectSessionDetailsPayload
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.list.model.SessionListModel

class WalletConnectSessionsViewModel(
    private val router: WalletConnectRouter,
    private val interactor: WalletConnectSessionInteractor,
    private val walletUiUseCase: WalletUiUseCase,
    private val walletConnectSessionMapper: WalletConnectSessionMapper,
) : BaseViewModel() {

    val sessionsFlow = interactor.activeSessionsFlow()
        .mapList(::mapSessionToUi)
        .shareInBackground()

    fun exit() {
        router.back()
    }

    fun newSessionClicked() {
        router.openScanPairingQrCode()
    }

    fun sessionClicked(item: SessionListModel) {
        router.openSessionDetails(WalletConnectSessionDetailsPayload(item.sessionTopic))
    }

    private suspend fun mapSessionToUi(session: WalletConnectSession): SessionListModel {
        val title = walletConnectSessionMapper.formatSessionDAppTitle(session.dappMetadata)

        return SessionListModel(
            dappTitle = title,
            walletModel = walletUiUseCase.walletUiFor(session.connectedMetaAccount),
            iconUrl = session.dappMetadata?.icon,
            sessionTopic = session.sessionTopic
        )
    }
}
