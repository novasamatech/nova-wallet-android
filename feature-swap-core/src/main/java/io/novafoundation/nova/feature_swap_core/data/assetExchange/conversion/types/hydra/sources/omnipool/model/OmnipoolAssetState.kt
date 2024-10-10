package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.model

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetId
import java.math.BigInteger

class OmnipoolAssetState(
    val tokenId: HydraDxAssetId,
    val hubReserve: BigInteger,
    val shares: BigInteger,
    val protocolShares: BigInteger,
    val tradeability: Tradeability
)

fun bindOmnipoolAssetState(decoded: Any?, tokenId: HydraDxAssetId): OmnipoolAssetState {
    val struct = decoded.castToStruct()

    return OmnipoolAssetState(
        tokenId = tokenId,
        hubReserve = bindNumber(struct["hubReserve"]),
        shares = bindNumber(struct["shares"]),
        protocolShares = bindNumber(struct["protocolShares"]),
        tradeability = bindTradeability(struct["tradable"])
    )
}
