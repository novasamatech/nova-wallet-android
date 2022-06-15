package io.novafoundation.nova.feature_staking_impl.presentation.common.hints

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor

class StakingHintsUseCase(
    private val resourceManager: ResourceManager,
    private val stakingInteractor: StakingInteractor,
) {

    fun redeemHint(): String {
        return resourceManager.getString(R.string.staking_hint_redeem_v2_2_0)
    }

    suspend fun unstakingDurationHint(): String {
        val lockupPeriod = stakingInteractor.getLockupDuration()
        val formattedDuration = resourceManager.formatDuration(lockupPeriod)

        return resourceManager.getString(R.string.staking_hint_unstake_format_v2_2_0, formattedDuration)
    }

    fun noRewardDurationUnstakingHint(): String {
        return resourceManager.getString(R.string.staking_hint_no_rewards_v2_2_0)
    }
}
