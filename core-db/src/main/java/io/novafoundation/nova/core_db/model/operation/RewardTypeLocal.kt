package io.novafoundation.nova.core_db.model.operation

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import io.novafoundation.nova.core_db.model.operation.OperationTypeLocal.OperationForeignKey
import java.math.BigInteger

sealed interface RewardTypeLocal : OperationTypeLocal {

    val isReward: Boolean

    val amount: BigInteger

    val eventId: String
}

@Entity(
    tableName = "operation_rewards_direct",
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
class DirectRewardTypeLocal(
    @Embedded
    override val foreignKey: OperationForeignKey,
    override val isReward: Boolean,
    override val amount: BigInteger,
    override val eventId: String,
    val era: Int?,
    val validator: String?,
) : RewardTypeLocal

class DirectRewardTypeJoin(
    val isReward: Boolean,
    val amount: BigInteger,
    val eventId: String,
    val era: Int,
    val validator: String?,
)

@Entity(
    tableName = "operation_rewards_pool",
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
class PoolRewardTypeLocal(
    @Embedded
    override val foreignKey: OperationForeignKey,
    override val isReward: Boolean,
    override val amount: BigInteger,
    override val eventId: String,
    val poolId: Int
) : RewardTypeLocal

class PoolRewardTypeJoin(
    val eventId: String,
    val isReward: Boolean,
    val amount: BigInteger,
    val poolId: Int
)
