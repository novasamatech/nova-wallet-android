package io.novafoundation.nova.feature_account_impl.data.multisig.blockhain.model

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindBlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindInt
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct

class OnChainMultisigTimePoint(
    val height: BlockNumber,
    val extrinsicIndex: Int
) {

    companion object {

        fun bind(decoded: Any?): OnChainMultisigTimePoint {
            val asStruct = decoded.castToStruct()

            return OnChainMultisigTimePoint(
                height = bindBlockNumber(asStruct["height"]),
                extrinsicIndex = bindInt(asStruct["index"])
            )
        }
    }
}
