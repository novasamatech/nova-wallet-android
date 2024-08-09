package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.feature_account_api.data.network.hydration.HydraDxAssetId
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

class OmnipoolAssetState(
    val tokenId: HydraDxAssetId,
    val hubReserve: Balance,
    val shares: Balance,
    val protocolShares: Balance,
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
