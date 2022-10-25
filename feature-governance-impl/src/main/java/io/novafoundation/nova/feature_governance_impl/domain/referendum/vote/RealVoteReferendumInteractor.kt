package io.novafoundation.nova.feature_governance_impl.domain.referendum.vote

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
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
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.asOngoingOrNull
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.completedReferendumLockDuration
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.flattenCastingVotes
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.maxLockDuration
import io.novafoundation.nova.feature_governance_api.data.repository.getTracksById
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumTrack
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.Change
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.GovernanceVoteAssistant
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.GovernanceVoteAssistant.LocksChange
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.GovernanceVoteAssistant.ReusableLock
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.VoteReferendumInteractor
import io.novafoundation.nova.feature_governance_impl.data.network.blockchain.extrinsic.convictionVotingVote
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLock
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Vote
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.blockDurationEstimator
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.chainAndAsset
import io.novafoundation.nova.runtime.util.BlockDurationEstimator
import jp.co.soramitsu.fearless_utils.hash.isPositive
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

private const val VOTE_ASSISTANT_CACHE_KEY = "RealVoteReferendumInteractor.VoteAssistant"

class RealVoteReferendumInteractor(
    private val governanceSourceRegistry: GovernanceSourceRegistry,
    private val chainStateRepository: ChainStateRepository,
    private val selectedChainState: SingleAssetSharedState,
    private val accountRepository: AccountRepository,
    private val extrinsicService: ExtrinsicService,
    private val locksRepository: BalanceLocksRepository,
    private val computationalCache: ComputationalCache,
) : VoteReferendumInteractor {

    override fun voteAssistantFlow(referendumId: ReferendumId, scope: CoroutineScope): Flow<GovernanceVoteAssistant> {
        return computationalCache.useSharedFlow(VOTE_ASSISTANT_CACHE_KEY, scope) {
            val (chain, chainAsset) = selectedChainState.chainAndAsset()
            val metaAccount = accountRepository.getSelectedMetaAccount()

            val voterAccountId = metaAccount.accountIdIn(chain)!!

            voteAssistantFlowSuspend(chain, chainAsset, voterAccountId, referendumId)
        }
    }

    override suspend fun estimateFee(amount: Balance, conviction: Conviction, referendumId: ReferendumId): Balance {
        val chain = selectedChainState.chain()
        val vote = AyeVote(amount, conviction) // vote direction does not influence fee estimation

        return extrinsicService.estimateFee(chain) {
            convictionVotingVote(referendumId, vote)
        }
    }

    override suspend fun vote(
        vote: AccountVote.Standard,
        referendumId: ReferendumId,
    ): Result<String> {
        val chain = selectedChainState.chain()
        return extrinsicService.submitExtrinsicWithSelectedWallet(chain) {
            convictionVotingVote(referendumId, vote)
        }
    }

    private suspend fun voteAssistantFlowSuspend(
        chain: Chain,
        chainAsset: Chain.Asset,
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

            Triple(locksByTrack, voting, votedReferenda)
        }

        val selectedReferendumFlow = governanceSource.referenda.onChainReferendumFlow(chain.id, referendumId)
        val balanceLocksFlow = locksRepository.observeBalanceLocks(chain, chainAsset)

        return combine(votingInformation, selectedReferendumFlow, balanceLocksFlow) { (locksByTrack, voting, votedReferenda), selectedReferendum, locks ->
            val blockDurationEstimator = chainStateRepository.blockDurationEstimator(chain.id)

            RealGovernanceLocksEstimator(
                onChainReferendum = selectedReferendum,
                balanceLocks = locks,
                governanceLocksByTrack = locksByTrack,
                voting = voting,
                votedReferenda = votedReferenda,
                blockDurationEstimator = blockDurationEstimator,
                tracks = tracks,
                undecidingTimeout = undecidingTimeout,
                voteLockingPeriod = voteLockingPeriod,
                votingLockId = "pyconvot"
            )
        }
    }
}

private class RealGovernanceLocksEstimator(
    override val onChainReferendum: OnChainReferendum,
    private val balanceLocks: List<BalanceLock>,
    private val governanceLocksByTrack: Map<TrackId, Balance>,
    private val voting: Map<TrackId, Voting>,
    private val votedReferenda: Map<ReferendumId, OnChainReferendum>,
    private val blockDurationEstimator: BlockDurationEstimator,
    private val tracks: Map<TrackId, TrackInfo>,
    private val undecidingTimeout: BlockNumber,
    private val voteLockingPeriod: BlockNumber,
    private val votingLockId: String,
) : GovernanceVoteAssistant {

    private val flattenedVotes = voting.flattenCastingVotes()

    private val currentMaxGovernanceLocked = governanceLocksByTrack.values.maxOrNull().orZero()
    private val currentMaxUnlocksAt = estimateUnlocksAt(changedVote = null)

    private val otherMaxLocked = balanceLocks.filter { it.id != votingLockId }
        .maxOfOrNull { it.amountInPlanks }
        .orZero()

    private val allMaxLocked = balanceLocks.maxOfOrNull { it.amountInPlanks }
        .orZero()

    override val track: ReferendumTrack? = onChainReferendum.status.asOngoingOrNull()?.let {
        ReferendumTrack(it.track, tracks.getValue(it.track).name)
    }

    override val trackVoting: Voting? = voting.findVotingFor(onChainReferendum)

    override suspend fun estimateLocksAfterVoting(
        amount: Balance,
        conviction: Conviction,
        asset: Asset,
    ): LocksChange {
        val vote = AyeVote(amount, conviction) // vote direction does not influence lock estimation

        val newGovernanceLocked = currentMaxGovernanceLocked.max(amount)
        val newMaxUnlocksAt = estimateUnlocksAt(changedVote = vote)
        val lockedDifference = newGovernanceLocked - currentMaxGovernanceLocked

        val previousLockDuration = blockDurationEstimator.durationUntil(currentMaxUnlocksAt)
        val newLockDuration = blockDurationEstimator.durationUntil(newMaxUnlocksAt)

        val currentTransferablePlanks = asset.transferableInPlanks
        val newLocked = otherMaxLocked.max(newGovernanceLocked)
        val newTransferablePlanks = asset.freeInPlanks - newLocked

        return LocksChange(
            lockedAmountChange = Change(
                previousValue = currentMaxGovernanceLocked,
                newValue = newGovernanceLocked,
                absoluteDifference = lockedDifference.abs(),
            ),
            lockedPeriodChange = Change(
                previousValue = previousLockDuration,
                newValue = newLockDuration,
                absoluteDifference = (newLockDuration - previousLockDuration).absoluteValue,
            ),
            transferableChange = Change(
                previousValue = currentTransferablePlanks,
                newValue = newTransferablePlanks,
                absoluteDifference = (newTransferablePlanks - currentTransferablePlanks).abs()
            )
        )
    }

    override suspend fun reusableLocks(): List<ReusableLock> {
        return buildList {
            addIfPositive(ReusableLock.Type.GOVERNANCE, currentMaxGovernanceLocked)
            addIfPositive(ReusableLock.Type.ALL, allMaxLocked)
        }
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
        return voting.values.filterIsInstance<Voting.Casting>()
            .maxOfOrNull { it.prior.unlockAt }.orZero()
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

    private fun Map<TrackId, Voting>.findVotingFor(onChainReferendum: OnChainReferendum): Voting? {
        val asOngoing = onChainReferendum.status.asOngoingOrNull()

        return if (asOngoing != null) {
            // fast-path - we have direct access to trackId
            get(asOngoing.track)
        } else {
            // slow path - referendum is completed so have to find by referendumId
            values.firstOrNull {
                it is Voting.Casting && onChainReferendum.id in it.votes.keys
            }
        }
    }

    private fun MutableList<ReusableLock>.addIfPositive(type: ReusableLock.Type, amount: Balance) {
        if (amount.isPositive()) {
            add(ReusableLock(type, amount))
        }
    }
}

private fun AyeVote(amount: Balance, conviction: Conviction) = AccountVote.Standard(
    vote = Vote(
        aye = true,
        conviction = conviction
    ),
    balance = amount
)
