package io.novafoundation.nova.feature_wallet_impl.data.mappers

import io.novafoundation.nova.common.utils.nullIfEmpty
import io.novafoundation.nova.core_db.model.OperationLocal
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.feature_wallet_impl.data.network.model.response.SubqueryHistoryElementResponse
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

fun mapOperationStatusToOperationLocalStatus(status: Operation.Status) = when (status) {
    Operation.Status.PENDING -> OperationLocal.Status.PENDING
    Operation.Status.COMPLETED -> OperationLocal.Status.COMPLETED
    Operation.Status.FAILED -> OperationLocal.Status.FAILED
}

private fun mapOperationStatusLocalToOperationStatus(status: OperationLocal.Status) = when (status) {
    OperationLocal.Status.PENDING -> Operation.Status.PENDING
    OperationLocal.Status.COMPLETED -> Operation.Status.COMPLETED
    OperationLocal.Status.FAILED -> Operation.Status.FAILED
}

private val Operation.Type.operationAmount
    get() = when (this) {
        is Operation.Type.Extrinsic -> null
        is Operation.Type.Reward -> amount
        is Operation.Type.Transfer -> amount
    }

private val Operation.Type.operationStatus
    get() = when (this) {
        is Operation.Type.Extrinsic -> status
        is Operation.Type.Reward -> Operation.Status.COMPLETED
        is Operation.Type.Transfer -> status
    }

private val Operation.Type.operationFee
    get() = when (this) {
        is Operation.Type.Extrinsic -> fee
        is Operation.Type.Reward -> null
        is Operation.Type.Transfer -> fee
    }

private val Operation.Type.hash
    get() = when (this) {
        is Operation.Type.Extrinsic -> hash
        is Operation.Type.Transfer -> hash
        is Operation.Type.Reward -> null
    }

private fun Operation.rewardOrNull() = type as? Operation.Type.Reward
private fun Operation.transferOrNull() = type as? Operation.Type.Transfer
private fun Operation.extrinsicOrNull() = type as? Operation.Type.Extrinsic

fun mapOperationToOperationLocalDb(
    operation: Operation,
    chainAsset: Chain.Asset,
    source: OperationLocal.Source,
): OperationLocal {
    val typeLocal = when (operation.type) {
        is Operation.Type.Transfer -> OperationLocal.Type.TRANSFER
        is Operation.Type.Reward -> OperationLocal.Type.REWARD
        is Operation.Type.Extrinsic -> OperationLocal.Type.EXTRINSIC
    }

    return with(operation) {
        OperationLocal(
            id = id,
            address = address,
            time = time,
            chainId = chainAsset.chainId,
            chainAssetId = chainAsset.id,
            module = extrinsicOrNull()?.module,
            call = extrinsicOrNull()?.call,
            amount = type.operationAmount,
            fee = type.operationFee,
            status = mapOperationStatusToOperationLocalStatus(type.operationStatus),
            source = source,
            operationType = typeLocal,
            sender = transferOrNull()?.sender,
            hash = type.hash,
            receiver = transferOrNull()?.receiver,
            isReward = rewardOrNull()?.isReward,
            era = rewardOrNull()?.era,
            validator = rewardOrNull()?.validator
        )
    }
}

fun mapOperationLocalToOperation(
    operationLocal: OperationLocal,
    chainAsset: Chain.Asset,
): Operation {
    with(operationLocal) {
        val operationType = when (operationType) {
            OperationLocal.Type.EXTRINSIC -> Operation.Type.Extrinsic(
                hash = hash!!,
                module = module!!,
                call = call!!,
                fee = fee!!,
                status = mapOperationStatusLocalToOperationStatus(status)
            )

            OperationLocal.Type.TRANSFER -> Operation.Type.Transfer(
                myAddress = address,
                amount = amount!!,
                receiver = receiver!!,
                sender = sender!!,
                fee = fee,
                status = mapOperationStatusLocalToOperationStatus(status),
                hash = hash
            )

            OperationLocal.Type.REWARD -> Operation.Type.Reward(
                amount = amount!!,
                isReward = isReward!!,
                era = era!!,
                validator = validator,
            )
        }

        return Operation(
            id = id,
            address = address,
            type = operationType,
            time = time,
            chainAsset = chainAsset,
        )
    }
}

@OptIn(ExperimentalTime::class)
fun mapNodeToOperation(
    node: SubqueryHistoryElementResponse.Query.HistoryElements.Node,
    tokenType: Chain.Asset,
): Operation {
    val type: Operation.Type = when {

        node.reward != null -> with(node.reward) {
            Operation.Type.Reward(
                amount = amount,
                era = era,
                isReward = isReward,
                validator = validator.nullIfEmpty()
            )
        }

        node.extrinsic != null -> with(node.extrinsic) {
            Operation.Type.Extrinsic(
                hash = hash,
                module = module,
                call = call,
                fee = fee,
                status = Operation.Status.fromSuccess(success)
            )
        }

        node.transfer != null -> with(node.transfer) {
            Operation.Type.Transfer(
                myAddress = node.address,
                amount = amount,
                receiver = to,
                sender = from,
                fee = fee,
                status = Operation.Status.fromSuccess(success),
                hash = extrinsicHash
            )
        }

        else -> throw IllegalStateException("All of the known operation type fields were null")
    }

    return Operation(
        id = node.id,
        address = node.address,
        type = type,
        time = node.timestamp.toLong().seconds.toLongMilliseconds(),
        chainAsset = tokenType,
    )
}
