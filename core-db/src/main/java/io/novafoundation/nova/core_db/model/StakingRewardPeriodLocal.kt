package io.novafoundation.nova.core_db.model

import androidx.room.Entity
import io.novasama.substrate_sdk_android.runtime.AccountId

@Entity(tableName = "staking_reward_period", primaryKeys = ["accountId", "chainId", "assetId", "stakingType"])
class StakingRewardPeriodLocal(
    val accountId: AccountId,
    val chainId: String,
    val assetId: Int,
    val stakingType: String,
    val periodType: String,
    val customPeriodStart: Long?,
    val customPeriodEnd: Long?
)
