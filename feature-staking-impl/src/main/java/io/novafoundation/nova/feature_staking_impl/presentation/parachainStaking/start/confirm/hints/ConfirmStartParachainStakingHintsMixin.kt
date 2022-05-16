package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.hints

import io.novafoundation.nova.common.mixin.hints.HintsMixin
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.ParachainStakingHintsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ConfirmStartParachainStakingHintsMixinFactory(
    private val stakingHintsUseCase: ParachainStakingHintsUseCase,
) {

    fun create(coroutineScope: CoroutineScope): HintsMixin = ConfirmStartParachainStakingHintsMixin(
        coroutineScope = coroutineScope,
        stakingHintsUseCase = stakingHintsUseCase
    )
}

private class ConfirmStartParachainStakingHintsMixin(
    private val stakingHintsUseCase: ParachainStakingHintsUseCase,
    coroutineScope: CoroutineScope
) : HintsMixin, WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    override val hintsFlow: Flow<List<String>> = combine(
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
