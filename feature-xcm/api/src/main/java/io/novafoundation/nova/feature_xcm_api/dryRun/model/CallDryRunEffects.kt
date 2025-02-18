package io.novafoundation.nova.feature_xcm_api.dryRun.model

import io.novafoundation.nova.common.data.network.runtime.binding.ScaleResult
import io.novafoundation.nova.common.data.network.runtime.binding.bindEvent
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.RuntimeContext
import io.novafoundation.nova.feature_xcm_api.message.VersionedRawXcmMessage
import io.novafoundation.nova.feature_xcm_api.message.bindVersionedRawXcmMessage
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent

class CallDryRunEffects(
    val executionResult: ScaleResult<DispatchPostInfo, DispatchErrorWithPostInfo>,
    val emittedEvents: List<GenericEvent.Instance>,
    // We don't need to fully decode/encode intermediate xcm messages
    val localXcm: VersionedRawXcmMessage?,
    val forwardedXcms: ForwardedXcms
) {

    companion object {

        context(RuntimeContext)
        fun bind(decodedInstance: Any?): CallDryRunEffects {
            val asStruct = decodedInstance.castToStruct()
            return CallDryRunEffects(
                executionResult = ScaleResult.bind(
                    dynamicInstance = asStruct["executionResult"],
                    bindOk = DispatchPostInfo::bind,
                    bindError = { DispatchErrorWithPostInfo.bind(it) }
                ),
                emittedEvents = bindList(asStruct["emittedEvents"], ::bindEvent),
                localXcm = asStruct.get<Any?>("localXcm")?.let { bindVersionedRawXcmMessage(it) },
                forwardedXcms = bindForwardedXcms(asStruct["forwardedXcms"])
            )
        }
    }
}
