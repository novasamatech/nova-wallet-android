package io.novafoundation.nova.feature_xcm_api.extrinsic

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.composeCall
import io.novafoundation.nova.feature_xcm_api.runtimeApi.dryRun.model.OriginCaller
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
