package io.novafoundation.nova.feature_multisig_operations.presentation.common

import android.os.Parcelable
import io.novafoundation.nova.feature_account_api.data.multisig.model.PendingMultisigOperationId
import kotlinx.parcelize.Parcelize

@Parcelize
class MultisigOperationPayload(
    val chainId: String,
    val metaId: Long,
    val callHash: String
) : Parcelable {
    companion object;
}

fun MultisigOperationPayload.Companion.fromOperationId(operationId: PendingMultisigOperationId): MultisigOperationPayload {
    return MultisigOperationPayload(
        chainId = operationId.chainId,
        metaId = operationId.metaId,
        callHash = operationId.callHash
    )
}

fun MultisigOperationPayload.toOperationId(): PendingMultisigOperationId {
    return PendingMultisigOperationId(
        chainId = chainId,
        metaId = metaId,
        callHash = callHash
    )
}
