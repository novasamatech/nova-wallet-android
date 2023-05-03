package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.relaychain

import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.BaseRewardComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.UserRewardsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards.UserRewardsState
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest

class RelaychainUserRewardsComponentFactory(
    private val stakingInteractor: StakingInteractor,
    private val stakingSharedComputation: StakingSharedComputation,
) {

    fun create(
        assetWithChain: ChainWithAsset,
        hostContext: ComponentHostContext
    ): UserRewardsComponent = RelaychainUserRewardsComponent(
        stakingInteractor = stakingInteractor,
        assetWithChain = assetWithChain,
        hostContext = hostContext,
        stakingSharedComputation = stakingSharedComputation,
    )
}

private class RelaychainUserRewardsComponent(
    private val stakingInteractor: StakingInteractor,
    private val stakingSharedComputation: StakingSharedComputation,
    private val assetWithChain: ChainWithAsset,
    private val hostContext: ComponentHostContext,
) : BaseRewardComponent(hostContext) {

    private val selectedAccountStakingStateFlow = stakingSharedComputation.selectedAccountStakingStateFlow(
        assetWithChain = assetWithChain,
        scope = hostContext.scope
    )

    override val state: Flow<UserRewardsState?> = selectedAccountStakingStateFlow.transformLatest { stakingState ->
        if (stakingState is StakingState.Stash) {
            emitAll(rewardsFlow(stakingState))
        } else {
            emit(null)
        }
    }
        .onStart { emit(null) }
        .shareInBackground()

    init {
        syncStakingRewards()
    }

    private fun rewardsFlow(stakingState: StakingState.Stash): Flow<UserRewardsState> = combine(
        stakingInteractor.observeUserRewards(stakingState, assetWithChain.chain, assetWithChain.asset),
        hostContext.assetFlow
    ) { totalReward, asset ->
        mapAmountToAmountModel(totalReward, asset)
    }.withLoading()

    private fun syncStakingRewards() {
        selectedAccountStakingStateFlow
            .filterIsInstance<StakingState.Stash>()
            .onEach { stakingInteractor.syncStakingRewards(it, assetWithChain.chain, assetWithChain.asset) }
            .inBackground()
            .launchIn(this)
    }
}
