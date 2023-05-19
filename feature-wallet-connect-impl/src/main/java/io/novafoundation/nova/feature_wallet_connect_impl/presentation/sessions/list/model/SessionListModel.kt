package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.list.model

import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletModel

data class SessionListModel(
    val dappTitle: String,
    val walletModel: WalletModel,
    val iconUrl: String?,
    val sessionTopic: String,
)
