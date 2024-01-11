package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

class OmnipoolAssetState(
    val hubReserve: Balance,
    val shares: Balance,
    val protocolShares: Balance,
    val tradeability: Tradeability
)

fun bindOmnipoolAssetState(decoded: Any?): OmnipoolAssetState {
    val struct = decoded.castToStruct()

    return OmnipoolAssetState(
        hubReserve = bindNumber(struct["hub_reserve"]),
        shares = bindNumber(struct["shares"]),
        protocolShares = bindNumber(struct["protocol_shares"]),
        tradeability = bindTradeability(struct["tradable"])
    )
}
