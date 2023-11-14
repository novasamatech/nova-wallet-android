package io.novafoundation.nova.core_db.model.operation

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import io.novafoundation.nova.core_db.model.AssetAndChainId
import io.novafoundation.nova.core_db.model.operation.OperationTypeLocal.OperationForeignKey
import java.math.BigInteger

@Entity(
    tableName = "operation_swaps",
    primaryKeys = ["operationId", "address", "chainId", "assetId"],
    indices = [
        Index("operationId", "address", "chainId", "assetId")
    ],
    foreignKeys = [
        ForeignKey(
            onDelete = ForeignKey.CASCADE,
            entity = OperationBaseLocal::class,
            parentColumns = ["id", "address", "chainId", "assetId"],
            childColumns = ["operationId", "address", "chainId", "assetId"]
        )
    ]
)
class SwapTypeLocal(
    @Embedded
    override val foreignKey: OperationForeignKey,
    @Embedded(prefix = "fee_")
    val fee: AssetWithAmount,
    @Embedded(prefix = "assetIn_")
    val assetIn: AssetWithAmount,
    @Embedded(prefix = "assetOut_")
    val assetOut: AssetWithAmount
) : OperationTypeLocal {

    class AssetWithAmount(
        @Embedded
        val assetId: AssetAndChainId,
        val amount: BigInteger
    )
}

class SwapTypeJoin(
    @Embedded(prefix = "fee_")
    val fee: SwapTypeLocal.AssetWithAmount,
    @Embedded(prefix = "assetIn_")
    val assetIn: SwapTypeLocal.AssetWithAmount,
    @Embedded(prefix = "assetOut_")
    val assetOut: SwapTypeLocal.AssetWithAmount
)
