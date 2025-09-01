package io.novafoundation.nova.feature_account_api.data.multisig

import io.novafoundation.nova.common.data.network.runtime.binding.WeightV2
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.composeCall
import io.novafoundation.nova.feature_account_api.data.multisig.model.MultisigTimePoint
import io.novafoundation.nova.feature_account_api.domain.model.MultisigMetaAccount
import io.novafoundation.nova.feature_account_api.domain.multisig.CallHash
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

fun RuntimeSnapshot.composeMultisigAsMulti(
    multisigMetaAccount: MultisigMetaAccount,
    maybeTimePoint: MultisigTimePoint?,
    call: GenericCall.Instance,
    maxWeight: WeightV2
): GenericCall.Instance {
    return composeCall(
        moduleName = Modules.MULTISIG,
        callName = "as_multi",
        arguments = mapOf(
            "threshold" to multisigMetaAccount.threshold.toBigInteger(),
            "other_signatories" to multisigMetaAccount.otherSignatories.map { it.value },
            "maybe_timepoint" to maybeTimePoint?.toEncodableInstance(),
            "call" to call,
            "max_weight" to maxWeight.toEncodableInstance()
        )
    )
}

fun RuntimeSnapshot.composeMultisigAsMultiThreshold1(
    multisigMetaAccount: MultisigMetaAccount,
    call: GenericCall.Instance,
): GenericCall.Instance {
    return composeCall(
        moduleName = Modules.MULTISIG,
        callName = "as_multi_threshold_1",
        arguments = mapOf(
            "other_signatories" to multisigMetaAccount.otherSignatories.map { it.value },
            "call" to call,
        )
    )
}

fun RuntimeSnapshot.composeMultisigCancelAsMulti(
    multisigMetaAccount: MultisigMetaAccount,
    maybeTimePoint: MultisigTimePoint,
    callHash: CallHash,
): GenericCall.Instance {
    return composeCall(
        moduleName = Modules.MULTISIG,
        callName = "cancel_as_multi",
        arguments = mapOf(
            "threshold" to multisigMetaAccount.threshold.toBigInteger(),
            "other_signatories" to multisigMetaAccount.otherSignatories.map { it.value },
            "timepoint" to maybeTimePoint.toEncodableInstance(),
            "call_hash" to callHash.value
        )
    )
}
