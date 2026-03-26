package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.model

import io.novafoundation.nova.common.data.network.runtime.binding.bindPermillFraction
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.Fraction

class SlipFeeConfig(
    val maxSlipFee: Fraction
) {

    companion object {

        fun bind(decoded: Any?): SlipFeeConfig {
            val asStruct = decoded.castToStruct()

            return SlipFeeConfig(
                maxSlipFee = bindPermillFraction(asStruct["maxSlipFee"])
            )
        }
    }
}
