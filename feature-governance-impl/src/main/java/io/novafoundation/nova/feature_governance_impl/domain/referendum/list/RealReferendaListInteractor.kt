package io.novafoundation.nova.feature_governance_impl.domain.referendum.list

import io.novafoundation.nova.common.address.get
import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
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
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.OffChainReferendumPreview
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.vote.UserVote
import io.novafoundation.nova.feature_governance_api.data.repository.PreImageRequest
import io.novafoundation.nova.feature_governance_api.data.repository.PreImageRequest.FetchCondition
import io.novafoundation.nova.feature_governance_api.data.repository.getTracksById
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSource
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import io.novafoundation.nova.feature_governance_api.data.source.trackLocksFlowOrEmpty
import io.novafoundation.nova.feature_governance_api.domain.locks.ClaimScheduleCalculator
import io.novafoundation.nova.feature_governance_api.domain.locks.RealClaimScheduleCalculator
import io.novafoundation.nova.feature_governance_api.domain.locks.hasClaimableLocks
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumTrack
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.DelegatedState
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.GovernanceLocksOverview
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendaListInteractor
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendaListState
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumGroup
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumPreview
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumProposal
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumStatus
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumVote
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.Voter
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.user
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.delegation.delegate.common.RECENT_VOTES_PERIOD
import io.novafoundation.nova.feature_governance_impl.domain.referendum.common.ReferendaConstructor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.list.sorting.ReferendaSortingProvider
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.blockDurationEstimator
import io.novafoundation.nova.runtime.state.selectedOption
import io.novafoundation.nova.runtime.util.blockInPast
import jp.co.soramitsu.fearless_utils.extensions.requireHexPrefix
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.hash.isPositive
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    private val referendaConstructor: ReferendaConstructor,
    private val referendaSortingProvider: ReferendaSortingProvider,
    private val identityRepository: OnChainIdentityRepository,
    private val governanceSharedState: GovernanceSharedState,
) : ReferendaListInteractor {

    override fun referendaListStateFlow(
        voterAccountId: AccountId?,
        selectedGovernanceOption: SupportedGovernanceOption
    ): Flow<ReferendaListState> {
        return flowOfAll {
            referendaListStateFlowSuspend(
                voter = voterAccountId?.let(Voter.Companion::user),
                selectedGovernanceOption = selectedGovernanceOption
            )
        }
    }

    override fun votedReferendaListFlow(voter: Voter, onlyRecentVotes: Boolean): Flow<List<ReferendumPreview>> {
        return flowOfAll {
            val selectedGovernanceOption = governanceSharedState.selectedOption()

            referendaListFlowSuspend(voter, selectedGovernanceOption, onlyRecentVotes)
        }
    }

    private suspend fun referendaListFlowSuspend(
        voter: Voter,
        selectedGovernanceOption: SupportedGovernanceOption,
        onlyRecentVotes: Boolean
    ): Flow<List<ReferendumPreview>> {
        val chain = selectedGovernanceOption.assetWithChain.chain

        val governanceSource = governanceSourceRegistry.sourceFor(selectedGovernanceOption)
        val tracksById = governanceSource.referenda.getTracksById(chain.id)

        return chainStateRepository.currentBlockNumberFlow(chain.id).map { currentBlockNumber ->
            coroutineScope {
                val onChainReferenda = async { governanceSource.referenda.getAllOnChainReferenda(chain.id) }
                val offChainInfo = async {
                    governanceSource.offChainInfo.referendumPreviews(chain)
                        .associateBy(OffChainReferendumPreview::referendumId)
                }
                val electorate = async { governanceSource.referenda.electorate(chain.id) }

                val onChainVoting = async { governanceSource.convictionVoting.votingFor(voter.accountId, chain.id) }

                val voterVotes = governanceSource.constructVoterVotes(voter, onChainVoting.await(), chain, onlyRecentVotes)

                val referenda = governanceSource.constructReferendumPreviews(
                    voting = voterVotes,
                    onChainReferenda = onChainReferenda.await(),
                    selectedGovernanceOption = selectedGovernanceOption,
                    tracksById = tracksById,
                    currentBlockNumber = currentBlockNumber,
                    offChainInfo = offChainInfo.await(),
                    electorate = electorate.await()
                )

                val sorting = referendaSortingProvider.getVotedReferendumSorting()
                referenda.onlyVoted().sortedWith(sorting)
            }
        }
    }

    private fun List<ReferendumPreview>.onlyVoted(): List<ReferendumPreview> {
        return filter { it.referendumVote != null }
    }

    private suspend fun referendaListStateFlowSuspend(
        voter: Voter?,
        selectedGovernanceOption: SupportedGovernanceOption
    ): Flow<ReferendaListState> {
        val chain = selectedGovernanceOption.assetWithChain.chain
        val asset = selectedGovernanceOption.assetWithChain.asset

        val governanceSource = governanceSourceRegistry.sourceFor(selectedGovernanceOption)
        val tracksById = governanceSource.referenda.getTracksById(chain.id)
        val undecidingTimeout = governanceSource.referenda.undecidingTimeout(chain.id)
        val voteLockingPeriod = governanceSource.convictionVoting.voteLockingPeriod(chain.id)
        val delegationSupported = governanceSource.delegationsRepository.isDelegationSupported()

        val trackLocksFlow = governanceSource.convictionVoting.trackLocksFlowOrEmpty(voter?.accountId, asset.fullId)

        val intermediateFlow = chainStateRepository.currentBlockNumberFlow(chain.id).map { currentBlockNumber ->
            coroutineScope {
                val onChainReferenda = async { governanceSource.referenda.getAllOnChainReferenda(chain.id) }
                val offChainInfo = async {
                    governanceSource.offChainInfo.referendumPreviews(chain)
                        .associateBy(OffChainReferendumPreview::referendumId)
                }
                val electorate = async { governanceSource.referenda.electorate(chain.id) }

                val onChainVoting = async { voter?.accountId?.let { governanceSource.convictionVoting.votingFor(it, chain.id) }.orEmpty() }

                val voterVotes = governanceSource.constructVoterVotes(voter, onChainVoting.await(), chain, onlyRecentVotes = false)

                val referenda = governanceSource.constructReferendumPreviews(
                    voting = voterVotes,
                    onChainReferenda = onChainReferenda.await(),
                    selectedGovernanceOption = selectedGovernanceOption,
                    tracksById = tracksById,
                    currentBlockNumber = currentBlockNumber,
                    offChainInfo = offChainInfo.await(),
                    electorate = electorate.await()
                )
                val sortedReferenda = sortReferendaPreviews(referenda)

                val onChainReferendaById = onChainReferenda.await().associateBy(OnChainReferendum::id)

                IntermediateData(onChainVoting.await(), currentBlockNumber, onChainReferendaById, sortedReferenda)
            }
        }

        return combine(intermediateFlow, trackLocksFlow) { intermediateData, trackLocks ->
            val claimScheduleCalculator = with(intermediateData) {
                RealClaimScheduleCalculator(voting, currentBlockNumber, onChainReferenda, tracksById, undecidingTimeout, voteLockingPeriod, trackLocks)
            }
            val locksOverview = claimScheduleCalculator.governanceLocksOverview()

            ReferendaListState(
                groupedReferenda = intermediateData.referenda,
                locksOverview = locksOverview,
                delegated = determineDelegatedState(intermediateData.voting, delegationSupported),
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
        voting: Map<ReferendumId, ReferendumVote>,
        onChainReferenda: Collection<OnChainReferendum>,
        selectedGovernanceOption: SupportedGovernanceOption,
        tracksById: Map<TrackId, TrackInfo>,
        currentBlockNumber: BlockNumber,
        offChainInfo: Map<ReferendumId, OffChainReferendumPreview>,
        electorate: BigInteger
    ): List<ReferendumPreview> {
        val proposals = constructReferendaProposals(onChainReferenda, selectedGovernanceOption.assetWithChain.chain)

        val votingsById = onChainReferenda.associateBy(
            keySelector = { it.id },
            valueTransform = {
                referendaConstructor.constructReferendumVoting(
                    referendum = it,
                    tracksById = tracksById,
                    currentBlockNumber = currentBlockNumber,
                    electorate = electorate
                )
            }
        )

        val statuses = referendaConstructor.constructReferendaStatuses(
            selectedGovernanceOption = selectedGovernanceOption,
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
                referendumVote = voting[onChainReferendum.id]
            )
        }
        return referenda
    }

    private suspend fun GovernanceSource.constructVoterVotes(
        voter: Voter?,
        onChainVoting: Map<TrackId, Voting>,
        chain: Chain,
        onlyRecentVotes: Boolean
    ): Map<ReferendumId, ReferendumVote> {
        return when (voter?.type) {
            null -> emptyMap()

            Voter.Type.USER -> {
                val historicalVoting = delegationsRepository.allHistoricalVotesOf(voter.accountId, chain).orEmpty()
                val delegatesAccountIds = historicalVoting.values.mapNotNull {
                    it.castOrNull<UserVote.Delegated>()?.delegate
                }
                val identities = identityRepository.getIdentitiesFromIds(delegatesAccountIds, chain.id)

                val offChainVotesHistory = historicalVoting.mapValues { (_, userVote) ->
                    when (userVote) {
                        is UserVote.Delegated -> ReferendumVote.Account(
                            who = userVote.delegate,
                            whoIdentity = identities[userVote.delegate]?.display?.let(::Identity),
                            vote = userVote.vote
                        )

                        is UserVote.Direct -> ReferendumVote.User(userVote.vote)
                    }
                }

                val onChainVotesHistory = onChainVoting.flattenCastingVotes().mapValues { (_, accountVote) ->
                    ReferendumVote.User(accountVote)
                }

                // priority is for on chain data
                offChainVotesHistory + onChainVotesHistory
            }

            Voter.Type.ACCOUNT -> {
                val recentVotesBlockThreshold = if (onlyRecentVotes) {
                    val blockTimeConverter = chainStateRepository.blockDurationEstimator(chain.id)
                    blockTimeConverter.blockInPast(RECENT_VOTES_PERIOD)
                } else {
                    null
                }

                val historicalVoting = delegationsRepository.directHistoricalVotesOf(voter.accountId, chain, recentVotesBlockThreshold)
                val identity = identityRepository.getIdentityFromId(chain.id, voter.accountId)
                    ?.display?.let(::Identity)

                val offChainVotes = historicalVoting.orEmpty().mapValues { (_, directVote) ->
                    ReferendumVote.Account(voter.accountId, identity, directVote.vote)
                }

                val onChainVotes = onChainVoting.flattenCastingVotes().mapValues { (_, accountVote) ->
                    ReferendumVote.Account(voter.accountId, identity, accountVote)
                }

                if (onlyRecentVotes) {
                    // we do not take on-chain votes with recent votes limitations since we cannot filter on-chain votes by time
                    offChainVotes
                } else {
                    offChainVotes + onChainVotes
                }
            }
        }
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

    private fun determineDelegatedState(voting: Map<TrackId, Voting>, delegationsSupported: Boolean): DelegatedState {
        if (!delegationsSupported) return DelegatedState.DelegationNotSupported

        val delegatedAmount = voting.values.filterIsInstance<Voting.Delegating>()
            .maxOfOrNull { it.amount }

        return if (delegatedAmount != null) {
            DelegatedState.Delegated(delegatedAmount)
        } else {
            DelegatedState.NotDelegated
        }
    }
}
