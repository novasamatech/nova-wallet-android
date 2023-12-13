package io.novafoundation.nova.feature_assets.data.mappers

import io.novafoundation.nova.core_db.model.AssetAndChainId
import io.novafoundation.nova.core_db.model.operation.DirectRewardTypeJoin
import io.novafoundation.nova.core_db.model.operation.DirectRewardTypeLocal
import io.novafoundation.nova.core_db.model.operation.ExtrinsicTypeJoin
import io.novafoundation.nova.core_db.model.operation.ExtrinsicTypeLocal
import io.novafoundation.nova.core_db.model.operation.OperationBaseLocal
import io.novafoundation.nova.core_db.model.operation.OperationJoin
import io.novafoundation.nova.core_db.model.operation.OperationLocal
import io.novafoundation.nova.core_db.model.operation.OperationTypeLocal.OperationForeignKey
import io.novafoundation.nova.core_db.model.operation.PoolRewardTypeJoin
import io.novafoundation.nova.core_db.model.operation.PoolRewardTypeLocal
import io.novafoundation.nova.core_db.model.operation.RewardTypeLocal
import io.novafoundation.nova.core_db.model.operation.SwapTypeJoin
import io.novafoundation.nova.core_db.model.operation.SwapTypeLocal
import io.novafoundation.nova.core_db.model.operation.TransferTypeJoin
import io.novafoundation.nova.core_db.model.operation.TransferTypeLocal
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapAssetWithAmountToLocal
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapOperationStatusToOperationLocalStatus
import io.novafoundation.nova.feature_wallet_api.domain.model.ChainAssetWithAmount
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRate
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation.Type
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation.Type.Extrinsic.Content
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation.Type.Reward.RewardKind
import io.novafoundation.nova.feature_wallet_api.domain.model.convertPlanks
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

private fun mapOperationStatusLocalToOperationStatus(status: OperationBaseLocal.Status) = when (status) {
    OperationBaseLocal.Status.PENDING -> Operation.Status.PENDING
    OperationBaseLocal.Status.COMPLETED -> Operation.Status.COMPLETED
    OperationBaseLocal.Status.FAILED -> Operation.Status.FAILED
}

fun mapOperationToOperationLocalDb(
    operation: Operation,
    source: OperationBaseLocal.Source,
): OperationLocal = with(operation) {
    val localAssetId = AssetAndChainId(chainAsset.chainId, chainAsset.id)
    val foreignKey = OperationForeignKey(id, address, localAssetId)
    val typeLocal = when (val operationType = operation.type) {
        is Type.Extrinsic -> mapExtrinsicToLocal(operationType, foreignKey)
        is Type.Reward -> mapRewardToLocal(operationType, foreignKey)
        is Type.Swap -> mapSwapToLocal(operationType, foreignKey)
        is Type.Transfer -> mapTransferToLocal(operationType, foreignKey)
    }

    val base = OperationBaseLocal(
        id = id,
        address = address,
        time = time,
        assetId = localAssetId,
        hash = extrinsicHash,
        status = mapOperationStatusToOperationLocalStatus(operation.status),
        source = source
    )

    OperationLocal(
        base = base,
        type = typeLocal
    )
}

fun mapOperationLocalToOperation(
    operationLocal: OperationJoin,
    chainAsset: Chain.Asset,
    chain: Chain,
    coinRate: CoinRate?,
): Operation? = with(operationLocal) {
    val operationType = when {
        operationLocal.transfer != null -> mapTransferFromLocal(operationLocal.transfer!!, chainAsset, coinRate, operationLocal.base.address)
        operationLocal.directReward != null -> mapDirectRewardFromLocal(operationLocal.directReward!!, chainAsset, coinRate)
        operationLocal.poolReward != null -> mapPoolRewardFromLocal(operationLocal.poolReward!!, chainAsset, coinRate)
        operationLocal.extrinsic != null -> mapExtrinsicFromLocal(operationLocal.extrinsic!!, chainAsset, coinRate)
        operationLocal.swap != null -> mapSwapFromLocal(operationLocal.swap!!, chainAsset, chain, coinRate)
        else -> null
    } ?: return@with null

    return Operation(
        id = base.id,
        address = base.address,
        type = operationType,
        time = base.time,
        chainAsset = chainAsset,
        extrinsicHash = base.hash,
        status = mapOperationStatusLocalToOperationStatus(base.status)
    )
}

private fun mapExtrinsicToLocal(
    extrinsic: Type.Extrinsic,
    foreignKey: OperationForeignKey
): ExtrinsicTypeLocal {
    return when (val content = extrinsic.content) {
        is Content.ContractCall -> ExtrinsicTypeLocal(
            foreignKey = foreignKey,
            contentType = ExtrinsicTypeLocal.ContentType.SMART_CONTRACT_CALL,
            module = content.contractAddress,
            call = content.function,
            fee = extrinsic.fee
        )

        is Content.SubstrateCall -> ExtrinsicTypeLocal(
            foreignKey = foreignKey,
            contentType = ExtrinsicTypeLocal.ContentType.SUBSTRATE_CALL,
            module = content.module,
            call = content.call,
            fee = extrinsic.fee
        )
    }
}

private fun mapTransferToLocal(
    transfer: Type.Transfer,
    foreignKey: OperationForeignKey
): TransferTypeLocal = with(transfer) {
    TransferTypeLocal(
        foreignKey = foreignKey,
        amount = amount,
        sender = sender,
        receiver = receiver,
        fee = fee
    )
}

private fun mapRewardToLocal(
    reward: Type.Reward,
    foreignKey: OperationForeignKey
): RewardTypeLocal = with(reward) {
    when (val kind = reward.kind) {
        is RewardKind.Direct -> DirectRewardTypeLocal(
            foreignKey = foreignKey,
            isReward = isReward,
            amount = amount,
            eventId = eventId,
            era = kind.era,
            validator = kind.validator
        )

        is RewardKind.Pool -> PoolRewardTypeLocal(
            foreignKey = foreignKey,
            isReward = isReward,
            amount = amount,
            eventId = eventId,
            poolId = kind.poolId
        )
    }
}

private fun mapSwapToLocal(
    swap: Type.Swap,
    foreignKey: OperationForeignKey
): SwapTypeLocal = with(swap) {
    SwapTypeLocal(
        foreignKey = foreignKey,
        fee = mapAssetWithAmountToLocal(fee),
        assetIn = mapAssetWithAmountToLocal(amountIn),
        assetOut = mapAssetWithAmountToLocal(amountOut),
    )
}

private fun mapExtrinsicFromLocal(
    local: ExtrinsicTypeJoin,
    chainAsset: Chain.Asset,
    coinRate: CoinRate?,
): Type.Extrinsic {
    val content = when (local.contentType) {
        ExtrinsicTypeLocal.ContentType.SUBSTRATE_CALL -> Content.SubstrateCall(
            module = local.module,
            call = local.call.orEmpty()
        )
        ExtrinsicTypeLocal.ContentType.SMART_CONTRACT_CALL -> Content.ContractCall(
            contractAddress = local.module,
            function = local.call
        )
    }

    return Type.Extrinsic(
        content = content,
        fee = local.fee,
        fiatFee = coinRate?.convertPlanks(chainAsset, local.fee)
    )
}

private fun mapDirectRewardFromLocal(
    local: DirectRewardTypeJoin,
    chainAsset: Chain.Asset,
    coinRate: CoinRate?,
): Type.Reward {
    return Type.Reward(
        amount = local.amount,
        isReward = local.isReward,
        eventId = local.eventId,
        kind = RewardKind.Direct(
            era = local.era,
            validator = local.validator
        ),
        fiatAmount = coinRate?.convertPlanks(chainAsset, local.amount)
    )
}

private fun mapPoolRewardFromLocal(
    local: PoolRewardTypeJoin,
    chainAsset: Chain.Asset,
    coinRate: CoinRate?,
): Type.Reward {
    return Type.Reward(
        amount = local.amount,
        isReward = local.isReward,
        eventId = local.eventId,
        kind = RewardKind.Pool(poolId = local.poolId),
        fiatAmount = coinRate?.convertPlanks(chainAsset, local.amount)
    )
}

private fun mapTransferFromLocal(
    local: TransferTypeJoin,
    chainAsset: Chain.Asset,
    coinRate: CoinRate?,
    myAddress: String,
): Type.Transfer {
    return Type.Transfer(
        amount = local.amount,
        myAddress = myAddress,
        receiver = local.receiver,
        sender = local.sender,
        fiatAmount = coinRate?.convertPlanks(chainAsset, local.amount),
        fee = local.fee
    )
}

private fun mapSwapFromLocal(
    local: SwapTypeJoin,
    chainAsset: Chain.Asset,
    chain: Chain,
    coinRate: CoinRate?,
): Type.Swap? {
    val amountIn = mapAssetWithAmountFromLocal(chain, local.assetIn) ?: return null
    val amountOut = mapAssetWithAmountFromLocal(chain, local.assetOut) ?: return null

    val amount = if (amountIn.chainAsset.fullId == chainAsset.fullId) amountIn.amount else amountOut.amount

    return Type.Swap(
        fee = mapAssetWithAmountFromLocal(chain, local.fee) ?: return null,
        amountIn = amountIn,
        amountOut = amountOut,
        fiatAmount = coinRate?.convertPlanks(chainAsset, amount),
    )
}

private fun mapAssetWithAmountFromLocal(
    chain: Chain,
    local: SwapTypeLocal.AssetWithAmount
): ChainAssetWithAmount? {
    val asset = chain.assetsById[local.assetId.assetId] ?: return null

    return ChainAssetWithAmount(
        chainAsset = asset,
        amount = local.amount
    )
}
