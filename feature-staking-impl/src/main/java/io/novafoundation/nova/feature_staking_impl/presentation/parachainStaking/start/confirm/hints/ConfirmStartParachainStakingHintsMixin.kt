package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.confirm.hints

import io.novafoundation.nova.common.mixin.hints.ConstantHintsMixin
import io.novafoundation.nova.common.mixin.hints.HintsMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.common.StartParachainStakingMode
import kotlinx.coroutines.CoroutineScope

class ConfirmStartParachainStakingHintsMixinFactory(
    private val resourceManager: ResourceManager,
) {

    fun create(
        coroutineScope: CoroutineScope,
        mode: StartParachainStakingMode,
    ): HintsMixin = ConfirmStartParachainStakingHintsMixin(
        coroutineScope = coroutineScope,
        resourceManager = resourceManager,
        mode = mode
    )
}

private class ConfirmStartParachainStakingHintsMixin(
    private val resourceManager: ResourceManager,
    private val mode: StartParachainStakingMode,
    coroutineScope: CoroutineScope
) : ConstantHintsMixin(coroutineScope) {

    override suspend fun getHints(): List<String> = when (mode) {
        StartParachainStakingMode.START -> startStakingHints()
        StartParachainStakingMode.BOND_MORE -> stakeMoreHints()
    }

    private fun stakeMoreHints() = listOf(
        resourceManager.getString(R.string.staking_parachain_stake_more_hint)
    )

    private fun startStakingHints(): List<String> = emptyList()
}
