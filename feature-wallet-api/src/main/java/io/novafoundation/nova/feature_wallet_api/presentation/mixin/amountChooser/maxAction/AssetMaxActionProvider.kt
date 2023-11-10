package io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.maxAction

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AssetMaxActionProvider(
    private val assetFlow: Flow<Asset?>,
    private val assetField: (Asset) -> Balance,
    private val allowMaxAction: Boolean
) : MaxActionProvider {

    override val maxAvailableForDisplay: Flow<Balance?> = assetFlow.map { it?.let(assetField) }

    override val maxAvailableForAction: Flow<MaxActionProvider.MaxAvailableForAction?> = assetFlow.map { asset ->
        if (allowMaxAction && asset != null) {
            val extractedBalance = assetField(asset)

            MaxActionProvider.MaxAvailableForAction(extractedBalance, asset.token.configuration)
        } else {
            null
        }
    }
}
