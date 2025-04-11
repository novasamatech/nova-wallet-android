package io.novafoundation.nova.feature_wallet_impl.data.network.blockchain.calls

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.composeCall
import io.novafoundation.nova.feature_xcm_api.dryRun.model.OriginCaller
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

fun RuntimeSnapshot.composeDispatchAs(
    call: GenericCall.Instance,
    origin: OriginCaller
): GenericCall.Instance {
    return composeCall(
        moduleName = Modules.UTILITY,
        callName = "dispatch_as",
        arguments = mapOf(
            "as_origin" to origin.toEncodableInstance(),
            "call" to call
        )
    )
}

fun RuntimeSnapshot.composeBatchAll(
    calls: List<GenericCall.Instance>,
): GenericCall.Instance {
    return composeCall(
        moduleName = Modules.UTILITY,
        callName = "batch_all",
        arguments = mapOf(
            "calls" to calls
        )
    )
}
