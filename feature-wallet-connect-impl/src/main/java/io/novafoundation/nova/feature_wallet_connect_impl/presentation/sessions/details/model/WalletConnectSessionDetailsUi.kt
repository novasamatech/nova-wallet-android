package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.details.model

import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletModel
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainListOverview
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi

class WalletConnectSessionDetailsUi(
    val dappTitle: String,
    val dappUrl: String?,
    val dappIcon: String?,
    val networksOverview: ChainListOverview,
    val networks: List<ChainUi>,
    val wallet: WalletModel,
)
