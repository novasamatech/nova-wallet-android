package io.novafoundation.nova.feature_account_api.data.multisig

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.WeightV2
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.composeCall
import io.novafoundation.nova.feature_account_api.data.multisig.model.MultisigTimePoint
import io.novafoundation.nova.feature_account_api.domain.multisig.CallHash
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

fun RuntimeSnapshot.composeMultisigAsMulti(
    threshold: Int,
    otherSignatories: List<AccountIdKey>,
    maybeTimePoint: MultisigTimePoint?,
    call: GenericCall.Instance,
    maxWeight: WeightV2
): GenericCall.Instance {
    return composeCall(
        moduleName = Modules.MULTISIG,
        callName = "as_multi",
        arguments = mapOf(
            "threshold" to threshold.toBigInteger(),
            "other_signatories" to otherSignatories.sorted().map { it.value },
            "maybe_timepoint" to maybeTimePoint?.toEncodableInstance(),
            "call" to call,
            "max_weight" to maxWeight.toEncodableInstance()
        )
    )
}


fun RuntimeSnapshot.composeMultisigCancelAsMulti(
    threshold: Int,
    otherSignatories: List<AccountIdKey>,
    maybeTimePoint: MultisigTimePoint,
    callHash: CallHash,
): GenericCall.Instance {
    return composeCall(
        moduleName = Modules.MULTISIG,
        callName = "cancel_as_multi",
        arguments = mapOf(
            "threshold" to threshold.toBigInteger(),
            "other_signatories" to otherSignatories.sorted().map { it.value },
            "maybe_timepoint" to maybeTimePoint.toEncodableInstance(),
            "call_hash" to callHash.value
        )
    )
}
