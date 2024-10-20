package io.novafoundation.nova.feature_assets.presentation.balance.list.model.items

import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.runtime.ext.fullId

data class TokenAssetUi(override val asset: AssetModel, val chain: ChainUi) : AssetRvItem {

    override val itemId: String = asset.token.configuration.fullId.toString()
}
