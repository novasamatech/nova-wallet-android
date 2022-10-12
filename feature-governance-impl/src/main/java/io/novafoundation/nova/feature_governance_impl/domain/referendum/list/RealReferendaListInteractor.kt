package io.novafoundation.nova.feature_governance_impl.domain.referendum.list

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Proposal
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.flattenCastingVotes
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.hash
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.proposal
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.track
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.OffChainReferendumPreview
import io.novafoundation.nova.feature_governance_api.data.repository.PreImageRepository
import io.novafoundation.nova.feature_governance_api.data.repository.PreImageRequest
import io.novafoundation.nova.feature_governance_api.data.repository.PreImageRequest.FetchCondition
import io.novafoundation.nova.feature_governance_api.data.repository.getTracksById
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumTrack
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendaListInteractor
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumGroup
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumPreview
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumProposal
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumStatus
import io.novafoundation.nova.feature_governance_impl.domain.referendum.common.ReferendaConstructor
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.TotalIssuanceRepository
import jp.co.soramitsu.fearless_utils.extensions.requireHexPrefix
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RealReferendaListInteractor(
    private val chainStateRepository: ChainStateRepository,
    private val governanceSourceRegistry: GovernanceSourceRegistry,
    private val totalIssuanceRepository: TotalIssuanceRepository,
    private val preImageRepository: PreImageRepository,
    private val referendaConstructor: ReferendaConstructor,
) : ReferendaListInteractor {

    override fun referendaFlow(voterAccountId: AccountId?, chain: Chain): Flow<GroupedList<ReferendumGroup, ReferendumPreview>> {
        return flowOfAll { referendaFlowSuspend(voterAccountId, chain) }
    }

    private suspend fun referendaFlowSuspend(voterAccountId: AccountId?, chain: Chain): Flow<GroupedList<ReferendumGroup, ReferendumPreview>> {
        val governanceSource = governanceSourceRegistry.sourceFor(chain.id)
        val tracksById = governanceSource.referenda.getTracksById(chain.id)

        return chainStateRepository.currentBlockNumberFlow(chain.id).map { currentBlockNumber ->
            val onChainReferenda = governanceSource.referenda.getOnChainReferenda(chain.id)
            val offChainInfo = governanceSource.offChainInfo.referendumPreviews(chain).associateBy(OffChainReferendumPreview::referendumId)
            val totalIssuance = totalIssuanceRepository.getTotalIssuance(chain.id)

            val statuses = referendaConstructor.constructReferendaStatuses(
                chain = chain,
                onChainReferenda = onChainReferenda,
                tracksById = tracksById,
                currentBlockNumber = currentBlockNumber
            )

            val proposals = constructReferendaProposals(onChainReferenda, chain)

            val userVotes = voterAccountId?.let {
                governanceSource.convictionVoting.votingFor(voterAccountId, chain.id)
            }?.flattenCastingVotes().orEmpty()

            val referenda = onChainReferenda.map { onChainReferendum ->
                ReferendumPreview(
                    id = onChainReferendum.id,
                    offChainMetadata = offChainInfo[onChainReferendum.id]?.let {
                        ReferendumPreview.OffChainMetadata(it.title)
                    },
                    onChainMetadata = proposals[onChainReferendum.id]
                        ?.let(ReferendumPreview::OnChainMetadata),
                    track = onChainReferendum.track()?.let { trackId ->
                        tracksById[trackId]?.let { trackInfo ->
                            ReferendumTrack(name = trackInfo.name)
                        }
                    },
                    status = statuses.getValue(onChainReferendum.id),
                    voting = referendaConstructor.constructReferendumVoting(
                        referendum = onChainReferendum,
                        tracksById = tracksById,
                        currentBlockNumber = currentBlockNumber,
                        totalIssuance = totalIssuance
                    ),
                    userVote = userVotes[onChainReferendum.id]
                )
            }

            referenda.groupBy { it.group() }
        }
    }

    // Attempts to use call-based by proposals by either taking inlined call or fetching preimage if it's size does not exceed threshold
    // Otherwise uses hash-based proposals
    private suspend fun constructReferendaProposals(
        onChainReferenda: Collection<OnChainReferendum>,
        chain: Chain,
    ): Map<ReferendumId, ReferendumProposal?> {
        val preImageRequestsToFetch = onChainReferenda.mapNotNull {
            when (val proposal = it.proposal()) {
                is Proposal.Lookup -> PreImageRequest(proposal.hash, knownSize = proposal.callLength, fetchIf = FetchCondition.SMALL_SIZE)
                is Proposal.Legacy -> PreImageRequest(proposal.hash, knownSize = null, fetchIf = FetchCondition.SMALL_SIZE)
                else -> null
            }
        }
        val preImages = preImageRepository.getPreimagesFor(preImageRequestsToFetch, chain.id)

        return onChainReferenda.associateBy(
            keySelector = { it.id },
            valueTransform = {
                when (val proposal = it.proposal()) {
                    is Proposal.Inline -> ReferendumProposal.Call(proposal.call)
                    is Proposal.Lookup, is Proposal.Legacy -> {
                        val hashHex = proposal.hash()!!.toHexString()
                        val preImage = preImages[hashHex]

                        if (preImage != null) {
                            ReferendumProposal.Call(preImage.call)
                        } else {
                            ReferendumProposal.Hash(hashHex.requireHexPrefix())
                        }
                    }
                    null -> null
                }
            }
        )
    }

    private fun ReferendumPreview.group(): ReferendumGroup {
        return when (status) {
            is ReferendumStatus.Executed,
            is ReferendumStatus.Approved,
            is ReferendumStatus.NotExecuted -> ReferendumGroup.COMPLETED

            else -> ReferendumGroup.ONGOING
        }
    }
}
