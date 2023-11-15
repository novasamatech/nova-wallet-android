package io.novafoundation.nova.feature_wallet_api.data.mappers

import io.novafoundation.nova.core_db.model.AssetAndChainId
import io.novafoundation.nova.core_db.model.operation.OperationBaseLocal
import io.novafoundation.nova.core_db.model.operation.SwapTypeLocal
import io.novafoundation.nova.feature_wallet_api.domain.model.ChainAssetWithAmount
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation

fun mapOperationStatusToOperationLocalStatus(status: Operation.Status) = when (status) {
    Operation.Status.PENDING -> OperationBaseLocal.Status.PENDING
    Operation.Status.COMPLETED -> OperationBaseLocal.Status.COMPLETED
    Operation.Status.FAILED -> OperationBaseLocal.Status.FAILED
}

fun mapAssetWithAmountToLocal(
    chainAssetWithAmount: ChainAssetWithAmount
): SwapTypeLocal.AssetWithAmount = with(chainAssetWithAmount) {
    return SwapTypeLocal.AssetWithAmount(
        assetId = AssetAndChainId(chainAsset.chainId, chainAsset.id),
        amount = amount
    )
}
