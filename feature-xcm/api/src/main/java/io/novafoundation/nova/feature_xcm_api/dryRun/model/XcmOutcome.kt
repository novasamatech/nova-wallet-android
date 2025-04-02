package io.novafoundation.nova.feature_xcm_api.dryRun.model

import io.novafoundation.nova.common.data.network.runtime.binding.Weight
import io.novafoundation.nova.common.data.network.runtime.binding.bindWeight
import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.incompatible
import io.novafoundation.nova.common.utils.scale.DynamicScaleInstance

sealed class XcmOutcome {

    companion object {

        fun bind(decodedInstance: Any?): XcmOutcome {
            val asEnum = decodedInstance.castToDictEnum()
            val value = asEnum.value.castToStruct()

            return when (asEnum.name) {
                "Complete" -> Complete(
                    used = bindWeight(value["used"])
                )

                "Incomplete" -> Incomplete(
                    used = bindWeight(value["used"]),
                    error = DynamicScaleInstance(value["error"])
                )

                "Error" -> Error(
                    error = DynamicScaleInstance(value["error"])
                )

                else -> incompatible()
            }
        }
    }

    class Complete(val used: Weight) : XcmOutcome()

    class Incomplete(val used: Weight, val error: DynamicScaleInstance) : XcmOutcome()

    class Error(val error: DynamicScaleInstance) : XcmOutcome()
}
