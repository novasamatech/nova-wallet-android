package io.novafoundation.nova.feature_wallet_api.data.mappers

import androidx.annotation.StringRes
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetModel
import java.math.BigDecimal

fun mapAssetToAssetModel(
    asset: Asset,
    resourceManager: ResourceManager,
    retrieveAmount: (Asset) -> BigDecimal = Asset::transferable,
    @StringRes patternId: Int? = R.string.common_available_format
): AssetModel {
    val amount = retrieveAmount(asset).formatTokenAmount(asset.token.configuration)
    val formattedAmount = patternId?.let { resourceManager.getString(patternId, amount) } ?: amount

    return with(asset) {
        AssetModel(
            chainId = asset.token.configuration.chainId,
            chainAssetId = asset.token.configuration.id,
            imageUrl = token.configuration.iconUrl,
            tokenName = token.configuration.name,
            tokenSymbol = token.configuration.symbol,
            assetBalance = formattedAmount
        )
    }
}
