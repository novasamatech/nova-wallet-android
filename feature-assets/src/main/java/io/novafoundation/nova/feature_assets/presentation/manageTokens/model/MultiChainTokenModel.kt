package io.novafoundation.nova.feature_assets.presentation.manageTokens.model

data class MultiChainTokenModel(
    val icon: String?,
    val symbol: String,
    val networks: String,
    val enabled: Boolean
)
