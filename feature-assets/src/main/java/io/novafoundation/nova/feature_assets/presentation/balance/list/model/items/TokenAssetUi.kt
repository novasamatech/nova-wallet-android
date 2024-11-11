package io.novafoundation.nova.feature_assets.presentation.balance.list.model.items

import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.recyclerView.expandable.items.ExpandableChildItem
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.runtime.ext.fullId

data class TokenAssetUi(
    override val groupId: String,
    override val asset: AssetModel,
    val assetIcon: Icon,
    val chain: ChainUi
) : AssetRvItem, ExpandableChildItem {

    override val itemId: String = "token_" + asset.token.configuration.fullId.toString()

    override fun equals(other: Any?): Boolean {
        if (other is TokenAssetUi) {
            return chain.id == other.chain.id
        }

        return false
    }
}
