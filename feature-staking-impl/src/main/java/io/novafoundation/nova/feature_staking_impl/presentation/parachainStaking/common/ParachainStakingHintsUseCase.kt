package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.RoundDurationEstimator
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@Suppress("OPT_IN_USAGE_FUTURE_ERROR")
class ParachainStakingHintsUseCase(
    private val singleAssetSharedState: AnySelectedAssetOptionSharedState,
    private val resourceManager: ResourceManager,
    private val roundDurationEstimator: RoundDurationEstimator,
) {

    fun rewardFrequencyHintFlow(): Flow<String> = flowWithChainId { chainId ->
        roundDurationEstimator.roundDurationFlow(chainId).map {
            val durationFormatted = resourceManager.formatDuration(it)

            resourceManager.getString(R.string.staking_parachain_reward_frequency_hint, durationFormatted)
        }
    }

    fun unstakeDurationHintFlow(): Flow<String> = flowWithChainId { chainId ->
        roundDurationEstimator.unstakeDurationFlow(chainId).map {
            val durationFormatted = resourceManager.formatDuration(it)

            resourceManager.getString(R.string.staking_hint_unstake_format_v2_2_0, durationFormatted)
        }
    }

    fun noRewardDuringUnstakingHint(): String {
        return resourceManager.getString(R.string.staking_hint_no_rewards_v2_2_0)
    }

    fun redeemHint(): String {
        return resourceManager.getString(R.string.staking_hint_redeem_v2_2_0)
    }

    private fun <T> flowWithChainId(producer: suspend (ChainId) -> Flow<T>): Flow<T> = flow {
        val chainId = singleAssetSharedState.chainId()

        emitAll(producer(chainId))
    }
}
