package io.novafoundation.nova.feature_account_api.data.multisig.model

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.toHex
import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.multisig.CallHash
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import java.math.BigInteger
import kotlin.time.Duration

class PendingMultisigOperation(
    val multisigMetaId: Long,
    val call: GenericCall.Instance?,
    val callHash: CallHash,
    val chain: Chain,
    val timePoint: MultisigTimePoint,
    val approvals: List<AccountIdKey>,
    val depositor: AccountIdKey,
    val deposit: BigInteger,
    val signatoryAccountId: AccountIdKey,
    val signatoryMetaId: Long,
    val threshold: Int,
    val timestamp: Duration,
) : Identifiable {

    val operationId = PendingMultisigOperationId(multisigMetaId, chain.id, callHash.toHex())

    override val identifier: String = operationId.identifier()

    override fun toString(): String {
        val callFormatted = if (call != null) {
            "${call.module.name}.${call.function.name}"
        } else {
            callHash.toHex()
        }

        return "Call: $callFormatted, Chain: ${chain.name}, Approvals: ${approvals.size}/$threshold, User action: ${userAction()}"
    }

    companion object
}

data class PendingMultisigOperationId(
    val metaId: Long,
    val chainId: ChainId,
    val callHash: String,
) {
    companion object;
}

fun PendingMultisigOperation.userAction(): MultisigAction {
    return when (signatoryAccountId) {
        depositor -> MultisigAction.CanReject
        !in approvals -> MultisigAction.CanApprove(
            isFinalApproval = approvals.size == threshold - 1
        )

        else -> MultisigAction.Signed
    }
}

fun PendingMultisigOperationId.identifier() = toString()

fun PendingMultisigOperationId.Companion.create(metaAccount: MetaAccount, chain: Chain, callHash: String): PendingMultisigOperationId {
    return PendingMultisigOperationId(metaAccount.id, chain.id, callHash)
}
