package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindBlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.RoundIndex
import java.math.BigInteger

class RoundInfo(
    // Current round index
    val current: RoundIndex,

    // The first block of the current round
    val first: BlockNumber,

    // / The length of the current round in number of blocks
    val length: BigInteger
)

fun bindRoundInfo(dynamic: Any?): RoundInfo {
    val asStruct = dynamic.castToStruct()

    return RoundInfo(
        current = bindRoundIndex(asStruct["current"]),
        first = bindBlockNumber(asStruct["first"]),
        length = bindNumber(asStruct["length"])
    )
}
