package io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindBlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

class MythReleaseRequest(
    val block: BlockNumber,
    val amount: Balance
)

fun bindMythReleaseRequest(decoded: Any?): MythReleaseRequest {
    val asStruct = decoded.castToStruct()

    return MythReleaseRequest(
        block = bindBlockNumber(asStruct["block"]),
        amount = bindNumber(asStruct["amount"])
    )
}

fun bindMythReleaseQueues(decoded: Any): List<MythReleaseRequest> {
    return bindList(decoded, ::bindMythReleaseRequest)
}
