package io.novafoundation.nova.feature_wallet_impl.data.mappers

import io.novafoundation.nova.common.utils.nullIfEmpty
import io.novafoundation.nova.core_db.model.OperationLocal
import io.novafoundation.nova.core_db.model.OperationLocal.ExtrinsicContentType
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRate
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation.Type
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation.Type.Extrinsic.Content
import io.novafoundation.nova.feature_wallet_api.domain.model.convertPlanks
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

private val Type.operationAmount
    get() = when (this) {
        is Type.Extrinsic -> null
        is Type.Reward -> amount
        is Type.Transfer -> amount
    }

private val Type.operationFiatAmount
    get() = when (this) {
        is Type.Extrinsic -> null
        is Type.Reward -> fiatAmount
        is Type.Transfer -> fiatAmount
    }

private val Type.operationStatus
    get() = when (this) {
        is Type.Extrinsic -> status
        is Type.Reward -> Operation.Status.COMPLETED
        is Type.Transfer -> status
    }

private val Type.operationFee
    get() = when (this) {
        is Type.Extrinsic -> fee
        is Type.Reward -> null
        is Type.Transfer -> fee
    }

private val Type.operationFiatFee
    get() = when (this) {
        is Type.Extrinsic -> fiatFee
        is Type.Reward -> null
        is Type.Transfer -> null
    }

private val Type.hash
    get() = when (this) {
        is Type.Extrinsic -> hash
        is Type.Transfer -> hash
        is Type.Reward -> null
    }

private fun Operation.rewardOrNull() = type as? Type.Reward
private fun Operation.transferOrNull() = type as? Type.Transfer
private fun Operation.extrinsicOrNull() = type as? Type.Extrinsic

private fun mapExtrinsicContentToLocal(content: Content): OperationLocal.ExtrinsicContent {
    return when (content) {
        is Content.SubstrateCall -> OperationLocal.ExtrinsicContent(
            type = ExtrinsicContentType.SUBSTRATE_CALL,
            module = content.module,
            call = content.call
        )

        is Content.ContractCall -> OperationLocal.ExtrinsicContent(
            type = ExtrinsicContentType.SMART_CONTRACT_CALL,
            module = content.contractAddress,
            call = content.function
        )
    }
}

private fun mapExtrinsicContentFromLocal(content: OperationLocal.ExtrinsicContent): Content {
    return when (content.type) {
        ExtrinsicContentType.SUBSTRATE_CALL -> Content.SubstrateCall(
            module = content.module!!,
            call = content.call!!
        )

        ExtrinsicContentType.SMART_CONTRACT_CALL -> Content.ContractCall(
            contractAddress = content.module!!,
            function = content.call
        )
    }
}

fun mapOperationToOperationLocalDb(
    operation: Operation,
    chainAsset: Chain.Asset,
    source: OperationLocal.Source,
): OperationLocal {
    val typeLocal = when (operation.type) {
        is Type.Transfer -> OperationLocal.Type.TRANSFER
        is Type.Reward -> OperationLocal.Type.REWARD
        is Type.Extrinsic -> OperationLocal.Type.EXTRINSIC
    }

    return with(operation) {
        OperationLocal(
            id = id,
            address = address,
            time = time,
            chainId = chainAsset.chainId,
            chainAssetId = chainAsset.id,
            extrinsicContent = operation.extrinsicOrNull()?.content?.let(::mapExtrinsicContentToLocal),
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
    coinRate: CoinRate?,
): Operation {
    with(operationLocal) {
        val operationType = when (operationType) {
            OperationLocal.Type.EXTRINSIC -> Type.Extrinsic(
                hash = hash!!,
                content = mapExtrinsicContentFromLocal(operationLocal.extrinsicContent!!),
                fee = fee!!,
                fiatFee = coinRate?.convertPlanks(chainAsset, fee!!),
                status = mapOperationStatusLocalToOperationStatus(status),
            )

            OperationLocal.Type.TRANSFER -> Type.Transfer(
                hash = hash,
                myAddress = address,
                amount = amount!!,
                fiatAmount = coinRate?.convertPlanks(chainAsset, amount!!),
                receiver = receiver!!,
                sender = sender!!,
                status = mapOperationStatusLocalToOperationStatus(status),
                fee = fee,
            )

            OperationLocal.Type.REWARD -> Type.Reward(
                amount = amount!!,
                fiatAmount = coinRate?.convertPlanks(chainAsset, amount!!),
                isReward = isReward!!,
                era = era!!,
                validator = validator
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
    coinRate: CoinRate?,
    chainAsset: Chain.Asset,
): Operation {
    val type: Type = when {
        node.reward != null -> with(node.reward) {
            Type.Reward(
                amount = amount,
                fiatAmount = coinRate?.convertPlanks(chainAsset, amount),
                era = era,
                isReward = isReward,
                validator = validator.nullIfEmpty()
            )
        }

        node.extrinsic != null -> with(node.extrinsic) {
            Type.Extrinsic(
                hash = node.extrinsicHash,
                content = Content.SubstrateCall(module, call),
                fee = fee,
                fiatFee = coinRate?.convertPlanks(chainAsset, fee), // TODO We must remove it if we will not use it in history
                status = Operation.Status.fromSuccess(success)
            )
        }

        node.transfer != null -> with(node.transfer) {
            Type.Transfer(
                myAddress = node.address,
                amount = amount,
                fiatAmount = coinRate?.convertPlanks(chainAsset, amount),
                receiver = to,
                sender = from,
                fee = fee,
                status = Operation.Status.fromSuccess(success),
                hash = node.extrinsicHash
            )
        }

        node.assetTransfer != null -> with(node.assetTransfer) {
            Type.Transfer(
                hash = node.extrinsicHash,
                myAddress = node.address,
                amount = amount,
                fiatAmount = coinRate?.convertPlanks(chainAsset, amount),
                receiver = to,
                sender = from,
                status = Operation.Status.fromSuccess(success),
                fee = fee,
            )
        }

        else -> throw IllegalStateException("All of the known operation type fields were null")
    }

    return Operation(
        id = node.id,
        address = node.address,
        type = type,
        time = node.timestamp.seconds.toLongMilliseconds(),
        chainAsset = chainAsset,
    )
}
