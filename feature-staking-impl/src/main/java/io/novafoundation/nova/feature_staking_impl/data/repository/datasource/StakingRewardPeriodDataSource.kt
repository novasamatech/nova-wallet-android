package io.novafoundation.nova.feature_staking_impl.data.repository.datasource

import io.novafoundation.nova.common.data.network.runtime.binding.castOrNull
import io.novafoundation.nova.core_db.dao.StakingRewardPeriodDao
import io.novafoundation.nova.core_db.model.StakingRewardPeriodLocal
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriod
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriod.CustomRange
import io.novafoundation.nova.feature_staking_impl.domain.period.RewardPeriodType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date

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
            customPeriodStart = rewardPeriod.castOrNull<CustomRange>()?.start?.time,
            customPeriodEnd = rewardPeriod.castOrNull<CustomRange>()?.end?.time,
        )
    }

    private fun mapToRewardPeriodFromLocal(period: StakingRewardPeriodLocal?): RewardPeriod {
        val rewardPeriodType = mapPeriodTypeFromLocal(period) ?: return RewardPeriod.AllTime

        return when (rewardPeriodType) {
            RewardPeriodType.AllTime -> RewardPeriod.AllTime

            is RewardPeriodType.Preset -> {
                val offsetFromCurrentDate = RewardPeriod.getPresetOffset(rewardPeriodType)
                RewardPeriod.OffsetFromCurrent(offsetFromCurrentDate, rewardPeriodType)
            }

            RewardPeriodType.Custom -> CustomRange(
                start = Date(period?.customPeriodStart ?: 0L),
                end = period?.customPeriodEnd?.let(::Date)
            )
        }
    }

    private fun mapPeriodTypeFromLocal(period: StakingRewardPeriodLocal?): RewardPeriodType? {
        return when (period?.periodType) {
            "ALL_TIME" -> RewardPeriodType.AllTime
            "WEEK" -> RewardPeriodType.Preset.WEEK
            "MONTH" -> RewardPeriodType.Preset.MONTH
            "QUARTER" -> RewardPeriodType.Preset.QUARTER
            "HALF_YEAR" -> RewardPeriodType.Preset.HALF_YEAR
            "YEAR" -> RewardPeriodType.Preset.YEAR
            "CUSTOM" -> RewardPeriodType.Custom
            else -> null
        }
    }

    private fun mapPeriodTypeToLocal(rewardPeriod: RewardPeriod): String {
        return when (rewardPeriod.type) {
            RewardPeriodType.AllTime -> "ALL_TIME"
            RewardPeriodType.Preset.WEEK -> "WEEK"
            RewardPeriodType.Preset.MONTH -> "MONTH"
            RewardPeriodType.Preset.QUARTER -> "QUARTER"
            RewardPeriodType.Preset.HALF_YEAR -> "HALF_YEAR"
            RewardPeriodType.Preset.YEAR -> "YEAR"
            RewardPeriodType.Custom -> "CUSTOM"
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
