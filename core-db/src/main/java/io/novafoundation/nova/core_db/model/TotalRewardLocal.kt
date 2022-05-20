package io.novafoundation.nova.core_db.model

import androidx.room.Entity
import java.math.BigInteger

@Entity(
    tableName = "total_reward",
    primaryKeys = ["chainId", "chainAssetId", "accountAddress"]
)
data class TotalRewardLocal(
    val accountAddress: String,
    val chainId: String,
    val chainAssetId: Int,
    val totalReward: BigInteger
)
