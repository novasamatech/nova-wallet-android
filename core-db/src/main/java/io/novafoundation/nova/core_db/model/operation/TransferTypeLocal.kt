package io.novafoundation.nova.core_db.model.operation

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import io.novafoundation.nova.core_db.model.operation.OperationTypeLocal.OperationForeignKey
import java.math.BigInteger

@Entity(
    tableName = "operation_transfers",
    primaryKeys = ["operationId", "address", "chainId", "assetId"],
    indices = [
        Index("operationId", "address", "chainId", "assetId")
    ],
    foreignKeys = [
        ForeignKey(
            entity = OperationBaseLocal::class,
            parentColumns = ["id", "address", "chainId", "assetId"],
            childColumns = ["operationId", "address", "chainId", "assetId"]
        )
    ]
)
class TransferTypeLocal(
    @Embedded
    override val foreignKey: OperationForeignKey,
    val amount: BigInteger,
    val sender: String,
    val receiver: String,
    val fee: BigInteger?
) : OperationTypeLocal

class TransferTypeJoin(
    val amount: BigInteger,
    val sender: String,
    val receiver: String,
    val fee: BigInteger?
)
