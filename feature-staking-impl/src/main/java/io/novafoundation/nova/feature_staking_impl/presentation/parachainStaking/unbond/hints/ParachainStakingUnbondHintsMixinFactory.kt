package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond.hints

import io.novafoundation.nova.common.mixin.hints.HintsMixin
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.ParachainStakingHintsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ParachainStakingUnbondHintsMixinFactory(
    private val stakingHintsUseCase: ParachainStakingHintsUseCase,
) {

    fun create(
        coroutineScope: CoroutineScope,
    ): HintsMixin = ParachainStakingUnbondHintsMixin(
        coroutineScope = coroutineScope,
        stakingHintsUseCase = stakingHintsUseCase,
    )
}

private class ParachainStakingUnbondHintsMixin(
    private val stakingHintsUseCase: ParachainStakingHintsUseCase,

    coroutineScope: CoroutineScope
) : HintsMixin, WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    override val hintsFlow: Flow<List<String>> = stakingHintsUseCase.unstakeDurationHintFlow().map { unstakeDurationHint ->
        listOf(
            unstakeDurationHint,
            stakingHintsUseCase.noRewardDuringUnstakingHint(),
            stakingHintsUseCase.redeemHint()
        )
    }.shareInBackground()
}
