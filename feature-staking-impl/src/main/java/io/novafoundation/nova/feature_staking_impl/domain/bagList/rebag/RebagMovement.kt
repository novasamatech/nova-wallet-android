package io.novafoundation.nova.feature_staking_impl.domain.bagList.rebag

import io.novafoundation.nova.common.utils.SemiUnboundedRange
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

typealias BagAmountBoundaries = SemiUnboundedRange<Balance>

class RebagMovement(
    val from: BagAmountBoundaries,
    val to: BagAmountBoundaries,
)
