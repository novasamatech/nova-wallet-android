package io.novafoundation.nova.feature_wallet_api.presentation.model

data class AssetModel(
    val chainId: String,
    val chainAssetId: Int,
    val imageUrl: String?,
    val tokenName: String,
    val tokenSymbol: String,
    val assetBalance: String
)
