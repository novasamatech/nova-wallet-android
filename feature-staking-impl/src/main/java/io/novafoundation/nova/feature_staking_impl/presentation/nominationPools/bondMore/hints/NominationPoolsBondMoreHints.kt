package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.bondMore.hints

import io.novafoundation.nova.common.mixin.hints.ConstantHintsMixin
import io.novafoundation.nova.common.mixin.hints.HintsMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.hints.NominationPoolHintsUseCase
import kotlinx.coroutines.CoroutineScope

class NominationPoolsBondMoreHintsFactory(
    private val nominationPoolHintsUseCase: NominationPoolHintsUseCase,
    private val resourceManager: ResourceManager,
) {

    fun create(coroutineScope: CoroutineScope): HintsMixin {
        return NominationPoolsBondMoreHints(nominationPoolHintsUseCase, resourceManager, coroutineScope)
    }
}

private class NominationPoolsBondMoreHints(
    private val nominationPoolHintsUseCase: NominationPoolHintsUseCase,
    private val resourceManager: ResourceManager,
    coroutineScope: CoroutineScope
) : ConstantHintsMixin(coroutineScope) {

    override suspend fun getHints(): List<String> {
        return listOfNotNull(
            increasedRewardsHint(),
            nominationPoolHintsUseCase.rewardsWillBeClaimedHint()
        )
    }

    private fun increasedRewardsHint(): String = resourceManager.getString(R.string.staking_hint_reward_bond_more_v2_2_0)
}
