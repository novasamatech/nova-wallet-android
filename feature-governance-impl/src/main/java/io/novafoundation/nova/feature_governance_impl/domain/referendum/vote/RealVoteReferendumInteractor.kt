package io.novafoundation.nova.feature_governance_impl.domain.referendum.vote

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendumStatus
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackInfo
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.completedReferendumLockDuration
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.flattenCastingVotes
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.maxLockDuration
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.onlyCasting
import io.novafoundation.nova.feature_governance_api.data.repository.getTracksById
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.Change
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.GovernanceVoteAssistant
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.GovernanceVoteAssistant.LocksChange
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.VoteReferendumInteractor
import io.novafoundation.nova.feature_governance_impl.data.network.blockchain.extrinsic.convictionVotingVote
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Vote
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.blockDurationEstimator
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.util.BlockDurationEstimator
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RealVoteReferendumInteractor(
    private val governanceSourceRegistry: GovernanceSourceRegistry,
    private val chainStateRepository: ChainStateRepository,
    private val selectedChainState: SingleAssetSharedState,
    private val accountRepository: AccountRepository,
    private val extrinsicService: ExtrinsicService
) : VoteReferendumInteractor {

    override fun voteAssistantFlow(referendumId: ReferendumId): Flow<GovernanceVoteAssistant> {
        return flowOfAll {
            val chain = selectedChainState.chain()
            val metaAccount = accountRepository.getSelectedMetaAccount()

            val voterAccountId = metaAccount.accountIdIn(chain)!!

            voteAssistantFlowSuspend(chain, voterAccountId, referendumId)
        }
    }

    override suspend fun estimateFee(amount: Balance, conviction: Conviction, referendumId: ReferendumId): Balance {
        val chain = selectedChainState.chain()
        val vote = AyeVote(amount, conviction) // vote direction does not influence fee estimation

        return extrinsicService.estimateFee(chain) {
            convictionVotingVote(referendumId, vote)
        }
    }

    override suspend fun vote(vote: AccountVote, referendumId: ReferendumId): Result<String> {
        val chain = selectedChainState.chain()

        return extrinsicService.submitExtrinsicWithSelectedWallet(chain) {
            convictionVotingVote(referendumId, vote)
        }
    }

    private suspend fun voteAssistantFlowSuspend(
        chain: Chain,
        voterAccountId: AccountId,
        referendumId: ReferendumId
    ): Flow<GovernanceVoteAssistant> {
        val governanceSource = governanceSourceRegistry.sourceFor(chain.id)
        val tracks = governanceSource.referenda.getTracksById(chain.id)
        val undecidingTimeout = governanceSource.referenda.undecidingTimeout(chain.id)
        val voteLockingPeriod = governanceSource.convictionVoting.voteLockingPeriod(chain.id)

        val votingInformation = governanceSource.convictionVoting.trackLocksFlow(voterAccountId, chain.id).map { locksByTrack ->
            val voting = governanceSource.convictionVoting.votingFor(voterAccountId, chain.id)
            val accountVotesByReferendumId = voting.flattenCastingVotes()
            val votedReferenda = governanceSource.referenda.getOnChainReferenda(chain.id, accountVotesByReferendumId.keys)

            val castingVotes = voting.onlyCasting()

            Triple(locksByTrack, castingVotes, votedReferenda)
        }

        val selectedReferendumFlow = governanceSource.referenda.onChainReferendumFlow(chain.id, referendumId)

        return combine(votingInformation, selectedReferendumFlow) { (locksByTrack, castingVotes, votedReferenda), selectedReferendum ->
            val blockDurationEstimator = chainStateRepository.blockDurationEstimator(chain.id)

            RealGovernanceLocksEstimator(
                onChainReferendum = selectedReferendum,
                locksByTrack = locksByTrack,
                voting = castingVotes,
                votedReferenda = votedReferenda,
                blockDurationEstimator = blockDurationEstimator,
                tracks = tracks,
                undecidingTimeout = undecidingTimeout,
                voteLockingPeriod = voteLockingPeriod
            )
        }
    }
}

private class RealGovernanceLocksEstimator(
    override val onChainReferendum: OnChainReferendum,
    private val locksByTrack: Map<TrackId, Balance>,
    private val voting: Map<TrackId, Voting.Casting>,
    private val votedReferenda: Map<ReferendumId, OnChainReferendum>,
    private val blockDurationEstimator: BlockDurationEstimator,
    private val tracks: Map<TrackId, TrackInfo>,
    private val undecidingTimeout: BlockNumber,
    private val voteLockingPeriod: BlockNumber,
) : GovernanceVoteAssistant {

    private val flattenedVotes = voting.flattenCastingVotes()

    private val currentMaxLocked = locksByTrack.values.maxOrNull().orZero()
    private val currentMaxUnlocksAt = estimateUnlocksAt(changedVote = null)

    override suspend fun estimateLocksAfterVoting(amount: Balance, conviction: Conviction): LocksChange {
        val vote = AyeVote(amount, conviction) // vote direction does not influence lock estimation

        val newLocked = currentMaxLocked.max(vote.balance)
        val newMaxUnlocksAt = estimateUnlocksAt(changedVote = vote)

        val lockedDifference = newLocked - currentMaxLocked

        val previousLockDuration = blockDurationEstimator.durationUntil(currentMaxUnlocksAt)
        val newLockDuration = blockDurationEstimator.durationUntil(newMaxUnlocksAt)

        return LocksChange(
            amountChange = Change(
                previousValue = currentMaxLocked,
                newValue = newLocked,
                absoluteDifference = lockedDifference.abs(),
            ),
            periodChange = Change(
                previousValue = previousLockDuration,
                newValue = newLockDuration,
                absoluteDifference = (newLockDuration - previousLockDuration).absoluteValue,
            )
        )
    }

    private fun estimateUnlocksAt(changedVote: AccountVote.Standard?): BlockNumber {
        val priorUnlocksAt = priorUnlocksAt()
        val votesEstimatedUnlocksAt = if (changedVote != null) {
            votesEstimatedUnlocksAt(changedVote)
        } else {
            votesEstimatedUnlocksAt()
        }

        return priorUnlocksAt.max(votesEstimatedUnlocksAt)
    }

    private fun priorUnlocksAt(): BlockNumber {
        return voting.values.maxOfOrNull { it.prior.unlockAt }.orZero()
    }

    private fun votesEstimatedUnlocksAt(changedVote: AccountVote.Standard): BlockNumber {
        val changedVoteMaxLock = onChainReferendum.maxConvictionEnd(changedVote)

        val currentVotesExceptChanged = votedReferenda.keys - onChainReferendum.id
        val currentVotesExceptChangedMaxUnlock = currentVotesExceptChanged.maxOfOrNull {
            val referendum = votedReferenda.getValue(it)
            val vote = flattenedVotes.getValue(it)

            referendum.maxConvictionEnd(vote)
        }.orZero()

        return changedVoteMaxLock.max(currentVotesExceptChangedMaxUnlock)
    }

    private fun votesEstimatedUnlocksAt(): BlockNumber {
        return votedReferenda.maxOfOrNull { (id, referendum) ->
            val vote = flattenedVotes.getValue(id)

            referendum.maxConvictionEnd(vote)
        }.orZero()
    }

    private fun OnChainReferendum.maxConvictionEnd(vote: AccountVote): BlockNumber {
        return when (val status = status) {
            is OnChainReferendumStatus.Ongoing -> status.maxConvictionEnd(vote)

            is OnChainReferendumStatus.Approved -> maxCompletedConvictionEnd(
                vote = vote,
                referendumOutcome = VoteType.AYE,
                completedSince = status.since
            )
            is OnChainReferendumStatus.Rejected -> maxCompletedConvictionEnd(
                vote = vote,
                referendumOutcome = VoteType.NAY,
                completedSince = status.since
            )

            is OnChainReferendumStatus.Cancelled -> status.since
            is OnChainReferendumStatus.Killed -> status.since
            is OnChainReferendumStatus.TimedOut -> status.since
        }
    }

    private fun maxCompletedConvictionEnd(
        vote: AccountVote,
        referendumOutcome: VoteType,
        completedSince: BlockNumber
    ): BlockNumber {
        val convictionPart = vote.completedReferendumLockDuration(referendumOutcome, voteLockingPeriod)

        return completedSince + convictionPart
    }

    private fun OnChainReferendumStatus.Ongoing.maxConvictionEnd(vote: AccountVote): BlockNumber {
        val trackInfo = tracks.getValue(track)
        val decisionPeriod = trackInfo.decisionPeriod

        val blocksAfterCompleted = vote.maxLockDuration(voteLockingPeriod)

        val maxCompletedAt = when {
            inQueue -> {
                val maxDecideSince = submitted + undecidingTimeout

                maxDecideSince + decisionPeriod
            }

            // confirming
            deciding?.confirming != null -> {
                val approveBlock = deciding!!.confirming!!.till
                val rejectBlock = deciding!!.since + decisionPeriod

                approveBlock.max(rejectBlock)
            }

            // rejecting
            deciding != null -> {
                val rejectBlock = deciding!!.since + decisionPeriod

                rejectBlock
            }

            // preparing
            else -> {
                val maxDecideSince = submitted + undecidingTimeout.max(trackInfo.preparePeriod)

                maxDecideSince + decisionPeriod
            }
        }

        return maxCompletedAt + blocksAfterCompleted
    }
}

private fun AyeVote(amount: Balance, conviction: Conviction) = AccountVote.Standard(
    vote = Vote(
        aye = true,
        conviction = conviction
    ),
    balance = amount
)
