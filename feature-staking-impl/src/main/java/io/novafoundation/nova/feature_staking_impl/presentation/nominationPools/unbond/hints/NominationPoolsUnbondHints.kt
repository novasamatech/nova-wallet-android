package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.unbond.hints

import io.novafoundation.nova.common.mixin.hints.ConstantHintsMixin
import io.novafoundation.nova.common.mixin.hints.HintsMixin
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.hints.NominationPoolHintsUseCase
import io.novafoundation.nova.feature_staking_impl.presentation.common.hints.StakingHintsUseCase
import kotlinx.coroutines.CoroutineScope

class NominationPoolsUnbondHintsFactory(
    private val nominationPoolHintsUseCase: NominationPoolHintsUseCase,
    private val stakingHintsUseCase: StakingHintsUseCase,
) {

    fun create(coroutineScope: CoroutineScope): HintsMixin {
        return NominationPoolsUnbondHints(nominationPoolHintsUseCase, stakingHintsUseCase, coroutineScope)
    }
}

private class NominationPoolsUnbondHints(
    private val nominationPoolHintsUseCase: NominationPoolHintsUseCase,
    private val stakingHintsUseCase: StakingHintsUseCase,
    coroutineScope: CoroutineScope
) : ConstantHintsMixin(coroutineScope) {

    override suspend fun getHints(): List<String> {
        return listOfNotNull(
            stakingHintsUseCase.unstakingDurationHint(coroutineScope = this),
            stakingHintsUseCase.noRewardDurationUnstakingHint(),
            stakingHintsUseCase.redeemHint(),
            nominationPoolHintsUseCase.rewardsWillBeClaimedHint()
        )
    }
}
