package io.novafoundation.nova.feature_wallet_api.presentation.model

import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class ChooseAmountModel(
    val input: ChooseAmountInputModel
)

class ChooseAmountInputModel(
    val tokenSymbol: String,
    val tokenIcon: Icon,
)

internal fun ChooseAmountModel(
    asset: Asset,
    assetIconProvider: AssetIconProvider
): ChooseAmountModel = ChooseAmountModel(
    input = ChooseAmountInputModel(asset.token.configuration, assetIconProvider)
)

internal fun ChooseAmountInputModel(chainAsset: Chain.Asset, assetIconProvider: AssetIconProvider): ChooseAmountInputModel = ChooseAmountInputModel(
    tokenSymbol = chainAsset.symbol.value,
    tokenIcon = assetIconProvider.getAssetIconOrFallback(chainAsset),
)
