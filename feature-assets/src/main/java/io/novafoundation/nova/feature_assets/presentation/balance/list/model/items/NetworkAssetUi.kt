package io.novafoundation.nova.feature_assets.presentation.balance.list.model.items

import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.runtime.ext.fullId

data class NetworkAssetUi(override val asset: AssetModel) : AssetRvItem {
    override val itemId: String = "network_" + asset.token.configuration.fullId.toString()
}
