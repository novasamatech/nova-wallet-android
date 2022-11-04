package io.novafoundation.nova.feature_governance_impl.domain.referendum.list

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Proposal
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackInfo
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.flattenCastingVotes
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.hash
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.proposal
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.track
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.OffChainReferendumPreview
import io.novafoundation.nova.feature_governance_api.data.repository.PreImageRequest
import io.novafoundation.nova.feature_governance_api.data.repository.PreImageRequest.FetchCondition
import io.novafoundation.nova.feature_governance_api.data.repository.getTracksById
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSource
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.data.source.trackLocksFlowOrEmpty
import io.novafoundation.nova.feature_governance_api.domain.locks.ClaimScheduleCalculator
import io.novafoundation.nova.feature_governance_api.domain.locks.RealClaimScheduleCalculator
import io.novafoundation.nova.feature_governance_api.domain.locks.hasClaimableLocks
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumTrack
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.GovernanceLocksOverview
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendaListInteractor
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendaListState
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumGroup
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumPreview
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumProposal
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumStatus
import io.novafoundation.nova.feature_governance_impl.domain.referendum.common.ReferendaConstructor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.list.sorting.ReferendaSortingProvider
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.TotalIssuanceRepository
import jp.co.soramitsu.fearless_utils.extensions.requireHexPrefix
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.hash.isPositive
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.math.BigInteger

private class IntermediateData(
    val voting: Map<TrackId, Voting>,
    val currentBlockNumber: BlockNumber,
    val onChainReferenda: Map<ReferendumId, OnChainReferendum>,
    val referenda: GroupedList<ReferendumGroup, ReferendumPreview>,
)

class RealReferendaListInteractor(
    private val chainStateRepository: ChainStateRepository,
    private val governanceSourceRegistry: GovernanceSourceRegistry,
    private val totalIssuanceRepository: TotalIssuanceRepository,
    private val referendaConstructor: ReferendaConstructor,
    private val referendaSortingProvider: ReferendaSortingProvider,
) : ReferendaListInteractor {

    override fun referendaListStateFlow(voterAccountId: AccountId?, chain: Chain, chainAsset: Chain.Asset): Flow<ReferendaListState> {
        return flowOfAll { referendaListStateFlowSuspend(voterAccountId, chain, chainAsset) }
    }

    private suspend fun referendaListStateFlowSuspend(voterAccountId: AccountId?, chain: Chain, asset: Chain.Asset): Flow<ReferendaListState> {
        val governanceSource = governanceSourceRegistry.sourceFor(chain.id)
        val tracksById = governanceSource.referenda.getTracksById(chain.id)
        val undecidingTimeout = governanceSource.referenda.undecidingTimeout(chain.id)
        val voteLockingPeriod = governanceSource.convictionVoting.voteLockingPeriod(chain.id)

        val trackLocksFlow = governanceSource.convictionVoting.trackLocksFlowOrEmpty(voterAccountId, asset.fullId)

        val intermediateFlow = chainStateRepository.currentBlockNumberFlow(chain.id).map { currentBlockNumber ->
            val onChainReferenda = governanceSource.referenda.getAllOnChainReferenda(chain.id)
            val offChainInfo = governanceSource.offChainInfo.referendumPreviews(chain)
                .associateBy(OffChainReferendumPreview::referendumId)
            val totalIssuance = totalIssuanceRepository.getTotalIssuance(chain.id)
            val voting = voterAccountId?.let { governanceSource.convictionVoting.votingFor(voterAccountId, chain.id) }.orEmpty()

            val referenda = governanceSource.constructReferendumPreviews(
                voting = voting,
                onChainReferenda = onChainReferenda,
                chain = chain,
                tracksById = tracksById,
                currentBlockNumber = currentBlockNumber,
                offChainInfo = offChainInfo,
                totalIssuance = totalIssuance
            )
            val sortedReferenda = sortReferendaPreviews(referenda)

            val onChainReferendaById = onChainReferenda.associateBy(OnChainReferendum::id)

            IntermediateData(voting, currentBlockNumber, onChainReferendaById, sortedReferenda)
        }

        return combine(intermediateFlow, trackLocksFlow) { intermediateData, trackLocks ->
            val claimScheduleCalculator = with(intermediateData) {
                RealClaimScheduleCalculator(voting, currentBlockNumber, onChainReferenda, tracksById, undecidingTimeout, voteLockingPeriod, trackLocks)
            }
            val locksOverview = claimScheduleCalculator.governanceLocksOverview()

            ReferendaListState(
                groupedReferenda = intermediateData.referenda,
                locksOverview = locksOverview
            )
        }
    }

    private fun ClaimScheduleCalculator.governanceLocksOverview(): GovernanceLocksOverview? {
        val totalLock = totalGovernanceLock()

        return if (totalLock.isPositive()) {
            val claimableSchedule = estimateClaimSchedule()

            GovernanceLocksOverview(
                locked = totalLock,
                hasClaimableLocks = claimableSchedule.hasClaimableLocks()
            )
        } else {
            null
        }
    }

    private suspend fun sortReferendaPreviews(referenda: List<ReferendumPreview>) =
        referenda.groupBy { it.group() }
            .mapValues { (group, referenda) ->
                val sorting = referendaSortingProvider.getReferendumSorting(group)

                referenda.sortedWith(sorting)
            }.toSortedMap(referendaSortingProvider.getGroupSorting())

    private suspend fun GovernanceSource.constructReferendumPreviews(
        voting: Map<TrackId, Voting>,
        onChainReferenda: Collection<OnChainReferendum>,
        chain: Chain,
        tracksById: Map<TrackId, TrackInfo>,
        currentBlockNumber: BlockNumber,
        offChainInfo: Map<ReferendumId, OffChainReferendumPreview>,
        totalIssuance: BigInteger
    ): List<ReferendumPreview> {
        val userVotes = voting.flattenCastingVotes()
        val proposals = constructReferendaProposals(onChainReferenda, chain)

        val votingsById = onChainReferenda.associateBy(
            keySelector = { it.id },
            valueTransform = {
                referendaConstructor.constructReferendumVoting(
                    referendum = it,
                    tracksById = tracksById,
                    currentBlockNumber = currentBlockNumber,
                    totalIssuance = totalIssuance
                )
            }
        )

        val statuses = referendaConstructor.constructReferendaStatuses(
            chain = chain,
            onChainReferenda = onChainReferenda,
            tracksById = tracksById,
            currentBlockNumber = currentBlockNumber,
            votingByReferenda = votingsById
        )

        val referenda = onChainReferenda.map { onChainReferendum ->
            ReferendumPreview(
                id = onChainReferendum.id,
                offChainMetadata = offChainInfo[onChainReferendum.id]?.title?.let {
                    ReferendumPreview.OffChainMetadata(it)
                },
                onChainMetadata = proposals[onChainReferendum.id]
                    ?.let(ReferendumPreview::OnChainMetadata),
                track = onChainReferendum.track()?.let { trackId ->
                    tracksById[trackId]?.let { trackInfo ->
                        ReferendumTrack(trackId, trackInfo.name, sameWithOther = tracksById.size == 1)
                    }
                },
                status = statuses.getValue(onChainReferendum.id),
                voting = votingsById[onChainReferendum.id],
                userVote = userVotes[onChainReferendum.id]
            )
        }
        return referenda
    }

    // Attempts to use call-based by proposals by either taking inlined call or fetching preimage if it's size does not exceed threshold
    // Otherwise uses hash-based proposals
    private suspend fun GovernanceSource.constructReferendaProposals(
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
                        val hashHex = proposal.hash().toHexString()
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
