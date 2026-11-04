package io.novafoundation.nova.feature_wallet_api.domain.model

import io.novafoundation.nova.common.utils.TokenSymbol

sealed class GetAssetOption {

    data object Receive : GetAssetOption()

    data object CrossChain : GetAssetOption()

    data object Buy : GetAssetOption()

    data class DApp(
        val name: String,
        val url: String,
        val supportedTokens: List<TokenSymbol>
    ) : GetAssetOption()
}
