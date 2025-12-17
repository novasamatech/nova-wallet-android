package io.novafoundation.nova.feature_wallet_api.presentation.model

import io.novafoundation.nova.common.presentation.masking.MaskableModel
import io.novafoundation.nova.common.utils.images.Icon

data class AssetModel(
    val chainId: String,
    val chainAssetId: Int,
    val icon: Icon,
    val tokenName: String,
    val tokenSymbol: String,
    val assetBalance: MaskableModel<String>
)
