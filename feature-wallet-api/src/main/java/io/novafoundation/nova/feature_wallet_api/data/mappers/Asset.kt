package io.novafoundation.nova.feature_wallet_api.data.mappers

import androidx.annotation.StringRes
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.masking.MaskableModel
import io.novafoundation.nova.common.presentation.masking.map
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetModel

@Deprecated("Create and use special formatter for that")
fun mapAssetToAssetModel(
    assetIconProvider: AssetIconProvider,
    asset: Asset,
    resourceManager: ResourceManager,
    maskableBalance: MaskableModel<Balance>,
    icon: Icon = assetIconProvider.getAssetIconOrFallback(asset.token.configuration),
    @StringRes patternId: Int? = R.string.common_available_format
): AssetModel {
    val formattedAmount = maskableBalance.map { it.formatPlanks(asset.token.configuration) }
        .map { amount -> patternId?.let { resourceManager.getString(patternId, amount) } ?: amount }

    return with(asset) {
        AssetModel(
            chainId = asset.token.configuration.chainId,
            chainAssetId = asset.token.configuration.id,
            icon = icon,
            tokenName = token.configuration.name,
            tokenSymbol = token.configuration.symbol.value,
            assetBalance = formattedAmount
        )
    }
}
