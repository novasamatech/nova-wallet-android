package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AssetMaxActionProvider(
    private val assetFlow: Flow<Asset>,
    private val assetField: suspend (Asset) -> Balance,
) : MaxActionProvider {

    override val maxAvailableBalance: Flow<MaxAvailableBalance> = assetFlow.map { asset ->
        val extractedBalance = assetField(asset)
        MaxAvailableBalance.fromSingle(asset.token.configuration, extractedBalance)
    }
}
