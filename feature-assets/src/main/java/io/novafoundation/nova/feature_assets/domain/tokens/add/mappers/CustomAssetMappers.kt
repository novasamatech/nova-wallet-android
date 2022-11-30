package io.novafoundation.nova.feature_assets.domain.tokens.add.mappers

import io.novafoundation.nova.feature_assets.domain.tokens.add.CoinGeckoLinkParser
import io.novafoundation.nova.feature_assets.domain.tokens.add.CustomErc20Token
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.chainAssetIdOfErc20Token
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

fun mapCustomTokenToChainAsset(customErc20Token: CustomErc20Token, coinGeckoLinkParser: CoinGeckoLinkParser): Chain.Asset {
    val priceId = coinGeckoLinkParser.parse(customErc20Token.priceLink).getOrNull()?.priceId

    return Chain.Asset(
        iconUrl = null,
        id = chainAssetIdOfErc20Token(customErc20Token.contract),
        priceId = priceId,
        chainId = customErc20Token.chainId,
        symbol = customErc20Token.symbol,
        precision = customErc20Token.decimals,
        buyProviders = emptyMap(),
        staking = Chain.Asset.StakingType.UNSUPPORTED,
        type = Chain.Asset.Type.Evm(customErc20Token.contract),
        source = Chain.Asset.Source.MANUAL,
        name = customErc20Token.symbol,
        enabled = true
    )
}
