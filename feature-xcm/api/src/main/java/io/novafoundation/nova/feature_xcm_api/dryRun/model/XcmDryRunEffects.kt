package io.novafoundation.nova.feature_xcm_api.dryRun.model

import io.novafoundation.nova.common.data.network.runtime.binding.bindEvent
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.RuntimeContext
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent

class XcmDryRunEffects(
    val executionResult: XcmOutcome,
    override val emittedEvents: List<GenericEvent.Instance>,
    override val forwardedXcms: ForwardedXcms
) : DryRunEffects {

    companion object {

        context(RuntimeContext)
        fun bind(decodedInstance: Any?): XcmDryRunEffects {
            val asStruct = decodedInstance.castToStruct()
            return XcmDryRunEffects(
                executionResult = XcmOutcome.bind(asStruct["executionResult"]),
                emittedEvents = bindList(asStruct["emittedEvents"], ::bindEvent),
                forwardedXcms = bindForwardedXcms(asStruct["forwardedXcms"])
            )
        }
    }
}
