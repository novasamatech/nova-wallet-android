package io.novafoundation.nova.feature_assets.presentation.flow.network.model

import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

data class NetworkFlowRvItem(
    val chainId: String,
    val assetId: Int,
    val networkName: String,
    val icon: String?,
    val balance: AmountModel
)
