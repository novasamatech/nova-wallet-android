package io.novafoundation.nova.feature_account_api.data.multisig.model

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.toHex
import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.feature_account_api.domain.multisig.CallHash
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall

class PendingMultisigOperation(
    val call: GenericCall.Instance?,
    val callHash: CallHash,
    val chain: Chain,
    val timePoint: MultisigTimePoint,
    val approvals: List<AccountIdKey>,
    val depositor: AccountIdKey,
    val signatoryAccountId: AccountIdKey,
    val signatoryMetaId: Long,
    val threshold: Int,
) : Identifiable {

    override val identifier: PendingMultisigOperationId = "${chain.id}.${callHash.toHex()}.${timePoint.height}.${timePoint.extrinsicIndex}"

    override fun toString(): String {
        val callFormatted = if (call != null) {
            "${call.module.name}.${call.function.name}"
        } else {
            callHash
        }

        return "Call: $callFormatted, Chain: ${chain.name}, Approvals: ${approvals.size}/$threshold, User action: ${userAction()}"
    }

    companion object
}

typealias PendingMultisigOperationId = String

fun PendingMultisigOperation.userAction(): MultisigAction {
    return when (signatoryAccountId) {
        depositor -> MultisigAction.CanReject
        !in approvals -> MultisigAction.CanApprove(
            isFinalApproval = approvals.size == threshold - 1
        )
        else -> MultisigAction.Signed
    }
}
