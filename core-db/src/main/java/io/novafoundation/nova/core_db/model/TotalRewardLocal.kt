package io.novafoundation.nova.core_db.model

import androidx.room.Entity
import io.novasama.substrate_sdk_android.runtime.AccountId
import java.math.BigInteger

@Entity(
    tableName = "total_reward",
    primaryKeys = ["chainId", "chainAssetId", "stakingType", "accountId"]
)
class TotalRewardLocal(
    val accountId: AccountId,
    val chainId: String,
    val chainAssetId: Int,
    val stakingType: String,
    val totalReward: BigInteger
)
