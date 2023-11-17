package io.novafoundation.nova.core_db.model.operation

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import io.novafoundation.nova.core_db.model.operation.OperationTypeLocal.OperationForeignKey
import java.math.BigInteger

@Entity(
    tableName = "operation_extrinsics",
    primaryKeys = ["operationId", "address", "chainId", "assetId"],
    indices = [
        Index("operationId", "address", "chainId", "assetId")
    ],
    foreignKeys = [
        ForeignKey(
            entity = OperationBaseLocal::class,
            parentColumns = ["id", "address", "chainId", "assetId"],
            childColumns = ["operationId", "address", "chainId", "assetId"],
            onDelete = ForeignKey.CASCADE,
            deferred = true,
        )
    ]
)
class ExtrinsicTypeLocal(
    @Embedded
    override val foreignKey: OperationForeignKey,
    val contentType: ContentType,
    val module: String,
    val call: String?,
    val fee: BigInteger,
) : OperationTypeLocal {

    enum class ContentType {
        SUBSTRATE_CALL, SMART_CONTRACT_CALL
    }
}

class ExtrinsicTypeJoin(
    val contentType: ExtrinsicTypeLocal.ContentType,
    val module: String,
    val call: String,
    val fee: BigInteger,
)
