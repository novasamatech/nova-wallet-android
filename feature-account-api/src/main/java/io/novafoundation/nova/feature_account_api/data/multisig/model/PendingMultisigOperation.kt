package io.novafoundation.nova.feature_account_api.data.multisig.model

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.domain.multisig.CallHash
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

class PendingMultisigOperation(
    val call: GenericCall.Instance?,
    val callHash: CallHash,
    val chain: Chain,
    val approvals: List<AccountIdKey>,
    val depositor: AccountIdKey,
    val signatory: AccountIdKey,
    val threshold: Int
) {

    override fun toString(): String {
        val callFormatted = if (call != null) {
            "${call.module.name}.${call.function.name}"
        } else {
            callHash
        }

        return "Call: $callFormatted, Chain: ${chain.name}, Approvals: ${approvals.size}/${threshold}, User action: ${userAction()}"
    }

    companion object
}

fun PendingMultisigOperation.userAction(): MultisigAction {
    return when (signatory) {
        depositor -> MultisigAction.CAN_REJECT
        !in approvals -> MultisigAction.CAN_APPROVE
        else -> MultisigAction.SIGNED
    }
}
