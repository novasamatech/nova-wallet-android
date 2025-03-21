package io.novafoundation.nova.feature_xcm_api.extrinsic

import io.novafoundation.nova.common.data.network.runtime.binding.WeightV2
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.composeCall
import io.novafoundation.nova.common.utils.xcmPalletName
import io.novafoundation.nova.feature_xcm_api.message.VersionedXcmMessage
import io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun.model.OriginCaller
import io.novafoundation.nova.feature_xcm_api.versions.toEncodableInstance
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

fun RuntimeSnapshot.composeDispatchAs(
    call: GenericCall.Instance,
    origin: OriginCaller
): GenericCall.Instance {
    return composeCall(
        moduleName = Modules.UTILITY,
        callName = "dispatch_as",
        args = mapOf(
            "as_origin" to origin.toEncodableInstance(),
            "call" to call
        )
    )
}

fun RuntimeSnapshot.composeXcmExecute(
    message: VersionedXcmMessage,
    maxWeight: WeightV2
): GenericCall.Instance {
    return composeCall(
        moduleName = metadata.xcmPalletName(),
        callName = "execute",
        args = mapOf(
            "message" to message.toEncodableInstance(),
            "max_weight" to maxWeight.toEncodableInstance()
        )
    )
}
