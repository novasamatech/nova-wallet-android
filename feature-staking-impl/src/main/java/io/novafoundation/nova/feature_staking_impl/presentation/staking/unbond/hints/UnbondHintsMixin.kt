package io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.hints

import io.novafoundation.nova.common.mixin.hints.ConstantHintsMixin
import io.novafoundation.nova.common.mixin.hints.HintsMixin
import io.novafoundation.nova.feature_staking_impl.presentation.common.hints.StakingHintsUseCase
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.CoroutineScope

class UnbondHintsMixinFactory(
    private val stakingHintsUseCase: StakingHintsUseCase,
) {

    fun create(
        coroutineScope: CoroutineScope,
        stashAccountIdProvider: suspend () -> AccountId
    ): HintsMixin = UnbondHintsMixin(
        coroutineScope = coroutineScope,
        stakingHintsUseCase = stakingHintsUseCase,
        stashAccountIdProvider = stashAccountIdProvider
    )
}

private class UnbondHintsMixin(
    coroutineScope: CoroutineScope,
    private val stakingHintsUseCase: StakingHintsUseCase,
    private val stashAccountIdProvider: suspend () -> AccountId,
) : ConstantHintsMixin(coroutineScope) {

    override suspend fun getHints(): List<String> = listOf(
        stakingHintsUseCase.unstakingDurationHint(coroutineScope, stashAccountIdProvider()),
        stakingHintsUseCase.noRewardDurationUnstakingHint(),
        stakingHintsUseCase.redeemHint(),
    )
}
