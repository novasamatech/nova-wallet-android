package io.novafoundation.nova.feature_dapp_impl.web3.states.hostApi

import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletModel

class AuthorizeDAppPayload(
    val title: String,
    val dAppIconUrl: String?,
    val dAppUrl: String,
    val walletModel: WalletModel
)
