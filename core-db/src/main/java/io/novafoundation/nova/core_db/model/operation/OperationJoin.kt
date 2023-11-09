package io.novafoundation.nova.core_db.model.operation

import androidx.room.Embedded

class OperationJoin(
    @Embedded(prefix = "o_")
    val base: OperationBaseLocal,
    @Embedded(prefix = "t_")
    val transfer: TransferTypeJoin?,
    @Embedded(prefix = "rd_")
    val directReward: DirectRewardTypeJoin?,
    @Embedded(prefix = "rp_")
    val poolReward: PoolRewardTypeJoin?,
    @Embedded(prefix = "s_")
    val swap: SwapTypeJoin?,
    @Embedded(prefix = "e_")
    val extrinsic: ExtrinsicTypeJoin?,
)
