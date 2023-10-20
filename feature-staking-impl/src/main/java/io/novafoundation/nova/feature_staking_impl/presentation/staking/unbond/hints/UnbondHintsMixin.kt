package io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.hints

import io.novafoundation.nova.common.mixin.hints.ConstantHintsMixin
import io.novafoundation.nova.common.mixin.hints.HintsMixin
import io.novafoundation.nova.feature_staking_impl.presentation.common.hints.StakingHintsUseCase
import kotlinx.coroutines.CoroutineScope

class UnbondHintsMixinFactory(
    private val stakingHintsUseCase: StakingHintsUseCase,
) {

    fun create(coroutineScope: CoroutineScope,): HintsMixin = UnbondHintsMixin(
        coroutineScope = coroutineScope,
        stakingHintsUseCase = stakingHintsUseCase
    )
}

private class UnbondHintsMixin(
    coroutineScope: CoroutineScope,
    private val stakingHintsUseCase: StakingHintsUseCase,
) : ConstantHintsMixin(coroutineScope) {

    override suspend fun getHints(): List<String> = listOf(
        stakingHintsUseCase.unstakingDurationHint(coroutineScope),
        stakingHintsUseCase.noRewardDurationUnstakingHint(),
        stakingHintsUseCase.redeemHint(),
    )
}
