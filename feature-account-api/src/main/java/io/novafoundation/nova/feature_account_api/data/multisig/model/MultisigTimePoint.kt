package io.novafoundation.nova.feature_account_api.data.multisig.model

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindBlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindInt
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.scale.ToDynamicScaleInstance
import io.novafoundation.nova.common.utils.structOf
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct

class MultisigTimePoint(
    val height: BlockNumber,
    val extrinsicIndex: Int
) : ToDynamicScaleInstance {

    companion object {

        fun bind(decoded: Any?): MultisigTimePoint {
            val asStruct = decoded.castToStruct()

            return MultisigTimePoint(
                height = bindBlockNumber(asStruct["height"]),
                extrinsicIndex = bindInt(asStruct["index"])
            )
        }
    }

    override fun toEncodableInstance(): Struct.Instance {
        return structOf(
            "height" to height,
            "index" to extrinsicIndex.toBigInteger()
        )
    }
}
