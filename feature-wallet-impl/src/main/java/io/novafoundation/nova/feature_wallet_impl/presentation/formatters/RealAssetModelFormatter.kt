package io.novafoundation.nova.feature_wallet_impl.presentation.formatters

import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.getAssetIconOrFallback
import io.novafoundation.nova.common.presentation.masking.MaskableModel
import io.novafoundation.nova.common.presentation.masking.map
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.AssetModelFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetModel
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class RealAssetModelFormatter(
    private val assetIconProvider: AssetIconProvider,
    private val resourceManager: ResourceManager
) : AssetModelFormatter {
    override suspend fun formatAsset(
        chainAsset: Chain.Asset,
        balance: MaskableModel<Balance>,
        patternId: Int?
    ): AssetModel {
        val icon = assetIconProvider.getAssetIconOrFallback(chainAsset.icon)
        val formattedAmount = balance.map { it.formatPlanks(chainAsset) }
            .map { amount -> patternId?.let { resourceManager.getString(patternId, amount) } ?: amount }

        return AssetModel(
            chainId = chainAsset.chainId,
            chainAssetId = chainAsset.id,
            icon = icon,
            tokenName = chainAsset.name,
            tokenSymbol = chainAsset.symbol.value,
            assetBalance = formattedAmount
        )
    }
}
