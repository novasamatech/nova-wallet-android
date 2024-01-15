package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.omnipool.model

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import java.math.BigInteger

typealias OmniPoolTokenId = BigInteger

class OmniPool(
    val tokens: Map<OmniPoolTokenId, OmniPoolToken>
)

class OmniPoolToken(
    val hubReserve: Balance,
    val shares: Balance,
    val protocolShares: Balance,
    val tradeability: Tradeability,
    val balance: Balance
)
