package io.novafoundation.nova.feature_wallet_connect_impl.presentation.mixin

import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_wallet_connect_api.domain.sessions.WalletConnectSessionsUseCase
import io.novafoundation.nova.feature_wallet_connect_api.presentation.WalletConnectSessionsModel
import io.novafoundation.nova.feature_wallet_connect_api.presentation.mapNumberOfActiveSessionsToUi
import io.novafoundation.nova.feature_wallet_connect_api.presentation.mixin.WalletConnectSessionsMixin
import io.novafoundation.nova.feature_wallet_connect_api.presentation.mixin.WalletConnectSessionsMixinFactory
import io.novafoundation.nova.feature_wallet_connect_impl.WalletConnectRouter
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.list.WalletConnectSessionsPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class RealWalletConnectSessionsMixinFactory(
    private val walletConnectSessionsUseCase: WalletConnectSessionsUseCase,
    private val router: WalletConnectRouter,
    private val selectedAccountUseCase: SelectedAccountUseCase
) : WalletConnectSessionsMixinFactory {
    override fun create(coroutineScope: CoroutineScope): WalletConnectSessionsMixin {
        return RealWalletConnectSessionsMixin(coroutineScope, walletConnectSessionsUseCase, router, selectedAccountUseCase)
    }
}

class RealWalletConnectSessionsMixin(
    private val coroutineScope: CoroutineScope,
    private val walletConnectSessionsUseCase: WalletConnectSessionsUseCase,
    private val router: WalletConnectRouter,
    private val selectedAccountUseCase: SelectedAccountUseCase
) : WalletConnectSessionsMixin, CoroutineScope by coroutineScope {

    private val selectedMetaAccount = selectedAccountUseCase.selectedMetaAccountFlow()

    private val walletConnectAccountSessionCount = selectedMetaAccount.flatMapLatest {
        walletConnectSessionsUseCase.activeSessionsNumberFlow(it)
    }
        .shareInBackground()

    override fun getActiveSessionsForSelectedAccount(): Flow<WalletConnectSessionsModel> {
        return walletConnectAccountSessionCount
            .map(::mapNumberOfActiveSessionsToUi)
            .shareInBackground()
    }

    override fun onWalletConnectClick() {
        coroutineScope.launch {
            if (walletConnectAccountSessionCount.first() > 0) {
                val metaAccount = selectedMetaAccount.first()
                val payload = WalletConnectSessionsPayload(metaAccount.id)
                router.openWalletConnectSessions(payload)
            } else {
                router.openScanPairingQrCode()
            }
        }
    }
}
