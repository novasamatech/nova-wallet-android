package io.novafoundation.nova.feature_staking_impl.data.repository.datasource

import io.novafoundation.nova.core_db.dao.StakingRewardPeriodDao
import io.novafoundation.nova.core_db.model.StakingRewardPeriodLocal
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriod
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriodType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.util.Date
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface StakingRewardPeriodDataSource {

    suspend fun setRewardPeriod(accountId: AccountId, chain: Chain, asset: Chain.Asset, stakingType: Chain.Asset.StakingType, rewardPeriod: RewardPeriod)

    suspend fun getRewardPeriod(accountId: AccountId, chain: Chain, asset: Chain.Asset, stakingType: Chain.Asset.StakingType): RewardPeriod

    fun observeRewardPeriod(accountId: AccountId, chain: Chain, asset: Chain.Asset, stakingType: Chain.Asset.StakingType): Flow<RewardPeriod>
}

class RealStakingRewardPeriodDataSource(
    private val dao: StakingRewardPeriodDao
) : StakingRewardPeriodDataSource {

    override suspend fun setRewardPeriod(
        accountId: AccountId,
        chain: Chain,
        asset: Chain.Asset,
        stakingType: Chain.Asset.StakingType,
        rewardPeriod: RewardPeriod
    ) {
        val localModel = mapRewardPeriodToLocal(accountId, chain.id, asset.id, stakingType, rewardPeriod)
        dao.insertStakingRewardPeriod(localModel)
    }

    override suspend fun getRewardPeriod(accountId: AccountId, chain: Chain, asset: Chain.Asset, stakingType: Chain.Asset.StakingType): RewardPeriod {
        val stakingTypeStr = mapStakingTypeToLocal(stakingType)
        val period = dao.getStakingRewardPeriod(accountId, chain.id, asset.id, stakingTypeStr)
        return mapToRewardPeriodFromLocal(period)
    }

    override fun observeRewardPeriod(accountId: AccountId, chain: Chain, asset: Chain.Asset, stakingType: Chain.Asset.StakingType): Flow<RewardPeriod> {
        val stakingTypeStr = mapStakingTypeToLocal(stakingType)
        return dao.observeStakingRewardPeriod(accountId, chain.id, asset.id, stakingTypeStr)
            .map { mapToRewardPeriodFromLocal(it) }
    }

    private fun mapRewardPeriodToLocal(
        accountId: AccountId,
        chainId: String,
        assetId: Int,
        stakingType: Chain.Asset.StakingType,
        rewardPeriod: RewardPeriod
    ): StakingRewardPeriodLocal {
        return StakingRewardPeriodLocal(
            chainId = chainId,
            assetId = assetId,
            accountId = accountId,
            stakingType = mapStakingTypeToLocal(stakingType),
            periodType = mapPeriodTypeToLocal(rewardPeriod),
            customPeriodStart = if (rewardPeriod is RewardPeriod.CustomRange) rewardPeriod.start.time else null,
            customPeriodEnd = if (rewardPeriod is RewardPeriod.CustomRange) rewardPeriod.end?.time else null,
        )
    }

    private fun mapToRewardPeriodFromLocal(period: StakingRewardPeriodLocal?): RewardPeriod {
        val rewardPeriodType = mapPeriodTypeFromLocal(period) ?: return RewardPeriod.AllTime
        val offsetFromCurrentDate = RewardPeriod.getOffsetByType(rewardPeriodType)
        return when (rewardPeriodType) {
            RewardPeriodType.ALL_TIME -> RewardPeriod.AllTime
            RewardPeriodType.WEEK,
            RewardPeriodType.MONTH,
            RewardPeriodType.QUARTER,
            RewardPeriodType.HALF_YEAR,
            RewardPeriodType.YEAR -> RewardPeriod.OffsetFromCurrent(offsetFromCurrentDate, rewardPeriodType)
            RewardPeriodType.CUSTOM -> RewardPeriod.CustomRange(
                Date(period?.customPeriodStart ?: 0L),
                period?.customPeriodEnd?.let { Date(it) }
            )
        }
    }

    private fun mapPeriodTypeFromLocal(period: StakingRewardPeriodLocal?): RewardPeriodType? {
        return when (period?.periodType) {
            "ALL_TIME" -> RewardPeriodType.ALL_TIME
            "WEEK" -> RewardPeriodType.WEEK
            "MONTH" -> RewardPeriodType.MONTH
            "QUARTER" -> RewardPeriodType.QUARTER
            "HALF_YEAR" -> RewardPeriodType.HALF_YEAR
            "YEAR" -> RewardPeriodType.YEAR
            "CUSTOM" -> RewardPeriodType.CUSTOM
            else -> null
        }
    }

    private fun mapPeriodTypeToLocal(rewardPeriod: RewardPeriod): String {
        return when (rewardPeriod.type) {
            RewardPeriodType.ALL_TIME -> "ALL_TIME"
            RewardPeriodType.WEEK -> "WEEK"
            RewardPeriodType.MONTH -> "MONTH"
            RewardPeriodType.QUARTER -> "QUARTER"
            RewardPeriodType.HALF_YEAR -> "HALF_YEAR"
            RewardPeriodType.YEAR -> "YEAR"
            RewardPeriodType.CUSTOM -> "CUSTOM"
        }
    }

    private fun mapStakingTypeToLocal(stakingType: Chain.Asset.StakingType): String {
        return when (stakingType) {
            Chain.Asset.StakingType.UNSUPPORTED -> "UNSUPPORTED"
            Chain.Asset.StakingType.ALEPH_ZERO -> "ALEPH_ZERO"
            Chain.Asset.StakingType.PARACHAIN -> "PARACHAIN"
            Chain.Asset.StakingType.RELAYCHAIN -> "RELAYCHAIN"
            Chain.Asset.StakingType.RELAYCHAIN_AURA -> "RELAYCHAIN_AURA"
            Chain.Asset.StakingType.TURING -> "TURING"
        }
    }
}
