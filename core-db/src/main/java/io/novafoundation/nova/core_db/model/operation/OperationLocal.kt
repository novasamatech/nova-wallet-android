package io.novafoundation.nova.core_db.model.operation

import io.novafoundation.nova.core_db.model.AssetAndChainId
import io.novafoundation.nova.core_db.model.operation.OperationTypeLocal.OperationForeignKey
import java.math.BigInteger

class OperationLocal(
    val base: OperationBaseLocal,
    val type: OperationTypeLocal
) {

    companion object {

        @Suppress("UnnecessaryVariable")
        fun manualTransfer(
            hash: String,
            address: String,
            chainId: String,
            chainAssetId: Int,
            amount: BigInteger,
            senderAddress: String,
            receiverAddress: String,
            fee: BigInteger?,
            status: OperationBaseLocal.Status,
            source: OperationBaseLocal.Source
        ): OperationLocal {
            val assetId = AssetAndChainId(chainId, chainAssetId)
            val id = hash

            return OperationLocal(
                base = OperationBaseLocal(
                    id = id,
                    address = address,
                    assetId = assetId,
                    time = System.currentTimeMillis(),
                    status = status,
                    source = source,
                    hash = hash
                ),
                type = TransferTypeLocal(
                    foreignKey = OperationForeignKey(
                        address = address,
                        operationId = id,
                        assetId = assetId
                    ),
                    sender = senderAddress,
                    receiver = receiverAddress,
                    fee = fee,
                    amount = amount,
                )
            )
        }

        @Suppress("UnnecessaryVariable")
        fun manualSwap(
            hash: String,
            originAddress: String,
            assetId: AssetAndChainId,
            fee: SwapTypeLocal.AssetWithAmount,
            amountIn: SwapTypeLocal.AssetWithAmount,
            amountOut: SwapTypeLocal.AssetWithAmount,
            status: OperationBaseLocal.Status,
            source: OperationBaseLocal.Source
        ): OperationLocal {
            val id = hash // we're ok here even if remote data source uses other ids since we clear operations by transactionHash in OperationDao

            return OperationLocal(
                base = OperationBaseLocal(
                    id = id,
                    address = originAddress,
                    assetId = assetId,
                    time = System.currentTimeMillis(),
                    status = status,
                    source = source,
                    hash = hash
                ),
                type = SwapTypeLocal(
                    foreignKey = OperationForeignKey(
                        address = originAddress,
                        operationId = id,
                        assetId = assetId
                    ),
                    fee = fee,
                    assetIn = amountIn,
                    assetOut = amountOut
                )
            )
        }
    }
}
