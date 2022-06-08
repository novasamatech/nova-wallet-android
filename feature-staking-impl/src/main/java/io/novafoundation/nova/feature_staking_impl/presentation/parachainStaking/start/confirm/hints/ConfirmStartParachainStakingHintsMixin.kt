package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.hints

import io.novafoundation.nova.common.mixin.hints.HintsMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.ParachainStakingHintsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class ConfirmStartParachainStakingHintsMixinFactory(
    private val stakingHintsUseCase: ParachainStakingHintsUseCase,
    private val resourceManager: ResourceManager,
) {

    fun create(
        coroutineScope: CoroutineScope,
        delegatorStateFlow: Flow<DelegatorState>
    ): HintsMixin = ConfirmStartParachainStakingHintsMixin(
        coroutineScope = coroutineScope,
        stakingHintsUseCase = stakingHintsUseCase,
        delegatorStateFlow = delegatorStateFlow,
        resourceManager = resourceManager
    )
}

private class ConfirmStartParachainStakingHintsMixin(
    private val stakingHintsUseCase: ParachainStakingHintsUseCase,
    private val resourceManager: ResourceManager,

    delegatorStateFlow: Flow<DelegatorState>,
    coroutineScope: CoroutineScope
) : HintsMixin, WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    override val hintsFlow: Flow<List<String>> = delegatorStateFlow.flatMapLatest {
        if (it is DelegatorState.Delegator) {
            stakeMoreHints()
        } else {
            startStakingHints()
        }
    }

    private fun stakeMoreHints() = flowOf(
        listOf(
            resourceManager.getString(R.string.staking_parachain_stake_more_hint)
        )
    )

    private fun startStakingHints() = combine(
        stakingHintsUseCase.rewardFrequencyHintFlow(),
        stakingHintsUseCase.unstakeDurationHintFlow()
    ) { rewardsHint, unstakeHints ->
        listOf(
            rewardsHint,
            unstakeHints,
            stakingHintsUseCase.noRewardDuringUnstakingHint(),
            stakingHintsUseCase.redeemHint()
        )
    }.shareInBackground()
}
