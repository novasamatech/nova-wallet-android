package io.novafoundation.nova.feature_assets.presentation.model

data class AssetModel(
    val token: TokenModel,
    val total: String,
    val priceAmount: String?
)
