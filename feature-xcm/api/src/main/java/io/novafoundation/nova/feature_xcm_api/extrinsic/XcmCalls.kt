package io.novafoundation.nova.feature_xcm_api.extrinsic

import io.novafoundation.nova.common.data.network.runtime.binding.WeightV2
import io.novafoundation.nova.common.utils.RuntimeContext
import io.novafoundation.nova.common.utils.composeCall
import io.novafoundation.nova.common.utils.xcmPalletName
import io.novafoundation.nova.feature_xcm_api.message.VersionedXcmMessage
import io.novafoundation.nova.feature_xcm_api.versions.toEncodableInstance
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

context(RuntimeContext)
fun composeXcmExecute(
    message: VersionedXcmMessage,
    maxWeight: WeightV2,
): GenericCall.Instance {
    return composeCall(
        moduleName = runtime.metadata.xcmPalletName(),
        callName = "execute",
        arguments = mapOf(
            "message" to message.toEncodableInstance(),
            "max_weight" to maxWeight.toEncodableInstance()
        )
    )
}
