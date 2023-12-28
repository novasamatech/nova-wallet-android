package io.novafoundation.nova.core_db.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import io.novafoundation.nova.core_db.model.chain.ChainAssetLocal
import io.novafoundation.nova.core_db.model.chain.ChainLocal
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import java.math.BigInteger

@Entity(
    tableName = "staking_dashboard_items",
    primaryKeys = ["chainId", "chainAssetId", "stakingType", "metaId"],
    foreignKeys = [
        ForeignKey(
            entity = ChainLocal::class,
            parentColumns = ["id"],
            childColumns = ["chainId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ChainAssetLocal::class,
            parentColumns = ["id", "chainId"],
            childColumns = ["chainAssetId", "chainId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MetaAccountLocal::class,
            parentColumns = ["id"],
            childColumns = ["metaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
)
class StakingDashboardItemLocal(
    val chainId: String,
    val chainAssetId: Int,
    val stakingType: String,
    @ColumnInfo(index = true) val metaId: Long,
    val hasStake: Boolean,
    val stake: BigInteger?,
    val status: Status?,
    val rewards: BigInteger?,
    val estimatedEarnings: Double?,
    val stakeStatusAccount: AccountId?,
    val rewardsAccount: AccountId?,
) {

    companion object {

        fun notStaking(
            chainId: String,
            chainAssetId: Int,
            stakingType: String,
            metaId: Long,
            estimatedEarnings: Double?
        ) = StakingDashboardItemLocal(
            chainId = chainId,
            chainAssetId = chainAssetId,
            stakingType = stakingType,
            metaId = metaId,
            hasStake = false,
            stake = null,
            status = null,
            rewards = null,
            estimatedEarnings = estimatedEarnings,
            stakeStatusAccount = null,
            rewardsAccount = null
        )

        fun staking(
            chainId: String,
            chainAssetId: Int,
            stakingType: String,
            stake: BigInteger,
            stakeStatusAccount: AccountId,
            rewardsAccount: AccountId,
            metaId: Long,
            status: Status?,
            rewards: BigInteger?,
            estimatedEarnings: Double?
        ) = StakingDashboardItemLocal(
            chainId = chainId,
            chainAssetId = chainAssetId,
            stakingType = stakingType,
            metaId = metaId,
            hasStake = true,
            stake = stake,
            status = status,
            rewards = rewards,
            estimatedEarnings = estimatedEarnings,
            stakeStatusAccount = stakeStatusAccount,
            rewardsAccount = rewardsAccount
        )
    }

    enum class Status {
        ACTIVE, INACTIVE, WAITING
    }
}

class StakingDashboardAccountsView(
    val chainId: String,
    val chainAssetId: Int,
    val stakingType: String,
    val stakeStatusAccount: AccountId?,
    val rewardsAccount: AccountId?
)
