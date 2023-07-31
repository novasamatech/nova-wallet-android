package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.nominationPools

import io.novafoundation.nova.common.presentation.flatMap
import io.novafoundation.nova.common.presentation.map
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.fullId
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.userRewards.NominationPoolsUserRewardsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.period.StakingRewardPeriodInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.period.mapRewardPeriodToString
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common.nominationPools.loadPoolMemberState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.BaseRewardComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.UserRewardsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.UserRewardsState
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import jp.co.soramitsu.fearless_utils.hash.isPositive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn

class NominationPoolUserRewardsComponentFactory(
    private val nominationPoolSharedComputation: NominationPoolSharedComputation,
    private val interactor: NominationPoolsUserRewardsInteractor,
    private val rewardPeriodsInteractor: StakingRewardPeriodInteractor,
    private val resourceManager: ResourceManager
) {

    fun create(
        stakingOption: StakingOption,
        hostContext: ComponentHostContext
    ): UserRewardsComponent = NominationPoolUserRewardsComponent(
        nominationPoolSharedComputation = nominationPoolSharedComputation,
        interactor = interactor,
        stakingOption = stakingOption,
        hostContext = hostContext,
        rewardPeriodsInteractor = rewardPeriodsInteractor,
        resourceManager = resourceManager
    )
}

private class NominationPoolUserRewardsComponent(
    private val nominationPoolSharedComputation: NominationPoolSharedComputation,
    private val interactor: NominationPoolsUserRewardsInteractor,
    private val stakingOption: StakingOption,
    private val hostContext: ComponentHostContext,
    private val rewardPeriodsInteractor: StakingRewardPeriodInteractor,
    private val resourceManager: ResourceManager
) : BaseRewardComponent(hostContext) {

    private val poolMemberDiffing = { old: PoolMember?, new: PoolMember? ->
        // we only care about accountId and rewardCounter being same, all other changes don't matter
        old?.accountId.contentEquals(new?.accountId) &&
            old?.lastRecordedRewardCounter.orZero() == new?.lastRecordedRewardCounter.orZero()
    }

    private val rewardPeriodState = rewardPeriodsInteractor.observeRewardPeriod(stakingOption)
        .shareInBackground()

    private val nominationPoolRewardsState = nominationPoolSharedComputation.loadPoolMemberState(
        hostContext = hostContext,
        chain = stakingOption.assetWithChain.chain,
        stateProducer = ::rewardsFlow,
        distinctUntilChanged = poolMemberDiffing,
    )

    init {
        launchRewardsSync()
    }

    override val state = combine(
        nominationPoolRewardsState,
        rewardPeriodState,
        hostContext.assetFlow
    ) { rewardsState, rewardPeriod, asset ->
        if (rewardsState == null) return@combine null

        val total = rewardsState.flatMap { poolRewards ->
            poolRewards.total.map { total -> mapAmountToAmountModel(total, asset) }
        }
        val claimable = rewardsState.flatMap { poolRewards ->
            poolRewards.claimable.map { claimable ->
                UserRewardsState.ClaimableRewards(
                    amountModel = mapAmountToAmountModel(claimable, asset) ,
                    canClaim = claimable.isPositive()
                )
            }
        }

        UserRewardsState(
            amount = total,
            claimableRewards = claimable,
            selectedRewardPeriod = mapRewardPeriodToString(resourceManager, rewardPeriod)
        )
    }
        .shareInBackground()

    private fun rewardsFlow(poolMember: PoolMember): Flow<NominationPoolsUserRewardsInteractor.NominationPoolRewards> {
        return interactor.rewardsFlow(poolMember.accountId, stakingOption.fullId)
    }

    private fun launchRewardsSync() {
        val poolMemberUpdates = nominationPoolSharedComputation.currentPoolMemberFlow(stakingOption.assetWithChain.chain, coroutineScope)
            .filterNotNull()
            .distinctUntilChanged(poolMemberDiffing)

        combine(
            rewardPeriodState,
            poolMemberUpdates
        ) { rewardPeriod, poolMember ->
            interactor.syncRewards(poolMember.accountId, stakingOption, rewardPeriod)
        }.launchIn(this)
    }
}
