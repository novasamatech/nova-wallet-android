package io.novafoundation.nova.feature_wallet_impl.presentation

import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload

interface WalletRouter {

    fun openSendCrossChain(destination: AssetPayload, recipientAddress: String?)

    fun openReceive(assetPayload: AssetPayload)

    fun openBuyToken(chainId: String, assetId: Int)
}
