package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.common

import io.novafoundation.nova.feature_governance_api.data.repository.common.RecentVotesDateThreshold
import io.novafoundation.nova.runtime.ext.hasTimelineChain
import io.novafoundation.nova.runtime.ext.timelineChainIdOrSelf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.blockDurationEstimator
import io.novafoundation.nova.runtime.util.blockInPast

class RecentVotesTimePointProvider(
    private val chainStateRepository: ChainStateRepository
) {

    suspend fun getTimePointThresholdForChain(chain: Chain): RecentVotesDateThreshold {
        return if (chain.hasTimelineChain()) {
            val timestampMs = System.currentTimeMillis() - RECENT_VOTES_PERIOD.inWholeMilliseconds
            RecentVotesDateThreshold.Timestamp(timestampMs)
        } else {
            val blockDurationEstimator = chainStateRepository.blockDurationEstimator(chain.timelineChainIdOrSelf())
            val recentVotesBlockThreshold = blockDurationEstimator.blockInPast(RECENT_VOTES_PERIOD)
            RecentVotesDateThreshold.BlockNumber(recentVotesBlockThreshold)
        }
    }
}
