package io.novafoundation.nova.feature_assets.domain.tokens.add

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class CustomErc20Token(
    val contract: String,
    val decimals: Int,
    val symbol: String,
    val priceLink: String,
    val chainId: ChainId,
)
