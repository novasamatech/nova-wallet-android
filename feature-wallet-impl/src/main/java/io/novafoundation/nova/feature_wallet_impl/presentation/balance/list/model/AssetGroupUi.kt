package io.novafoundation.nova.feature_wallet_impl.presentation.balance.list.model

import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi

data class AssetGroupUi(
    val chainUi: ChainUi,
    val groupBalanceFiat: String
)
