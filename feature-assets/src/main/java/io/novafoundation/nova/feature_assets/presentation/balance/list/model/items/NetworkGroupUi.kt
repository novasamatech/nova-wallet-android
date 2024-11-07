package io.novafoundation.nova.feature_assets.presentation.balance.list.model.items

import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi

data class NetworkGroupUi(
    val chainUi: ChainUi,
    val groupBalanceFiat: CharSequence
) : AssetGroupRvItem {

    override val itemId: String = chainUi.id
}
