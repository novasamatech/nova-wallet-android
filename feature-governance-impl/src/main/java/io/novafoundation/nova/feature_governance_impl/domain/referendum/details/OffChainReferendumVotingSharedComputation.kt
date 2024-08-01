package io.novafoundation.nova.feature_governance_impl.domain.referendum.details

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.isFinished
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.OffChainReferendumVotingDetails
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import kotlinx.coroutines.CoroutineScope

class OffChainReferendumVotingSharedComputation(
    private val computationalCache: ComputationalCache,
    private val governanceSourceRegistry: GovernanceSourceRegistry
) {

    fun hasCache(
        onChainReferendum: OnChainReferendum,
        governanceOption: SupportedGovernanceOption
    ): Boolean {
        val key = buildKey(onChainReferendum, governanceOption)
        return computationalCache.hasCache(key)
    }

    suspend fun votingDetails(
        onChainReferendum: OnChainReferendum,
        governanceOption: SupportedGovernanceOption,
        scope: CoroutineScope
    ): OffChainReferendumVotingDetails? {
        val referendumId = onChainReferendum.id
        val isReferendumFinished = onChainReferendum.isFinished()
        val key = buildKey(onChainReferendum, governanceOption)
        return computationalCache.useCache(key, scope) {
            if (isReferendumFinished) {
                votingStatusForFinishedReferendum(governanceOption, referendumId)
            } else {
                votingStatusForOngoingReferendum(governanceOption, referendumId)
            }
        }
    }

    private suspend fun votingStatusForOngoingReferendum(
        governanceOption: SupportedGovernanceOption,
        referendumId: ReferendumId
    ): OffChainReferendumVotingDetails? {
        return governanceSourceRegistry.sourceFor(governanceOption)
            .convictionVoting
            .abstainVotingDetails(referendumId, governanceOption.assetWithChain.chain)
    }

    private suspend fun votingStatusForFinishedReferendum(
        governanceOption: SupportedGovernanceOption,
        referendumId: ReferendumId
    ): OffChainReferendumVotingDetails? {
        return governanceSourceRegistry.sourceFor(governanceOption)
            .convictionVoting
            .fullVotingDetails(referendumId, governanceOption.assetWithChain.chain)
    }

    private fun buildKey(
        onChainReferendum: OnChainReferendum,
        governanceOption: SupportedGovernanceOption
    ): String {
        val referendumId = onChainReferendum.id
        val isReferendumFinished = onChainReferendum.isFinished()
        val chainId = governanceOption.assetWithChain.chain.id
        val assetId = governanceOption.assetWithChain.asset.id
        val version = governanceOption.additional.governanceType.name
        return "REFERENDUM_VOTING:$referendumId:$isReferendumFinished:$chainId:$assetId:$version"
    }
}
