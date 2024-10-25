package io.novafoundation.nova.feature_assets.presentation.balance.list.model.items

import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi

data class NetworkGroupUi(
    val chainUi: ChainUi,
    val groupBalanceFiat: String
) : AssetGroupRvItem {

    override val id: String = chainUi.id
}
