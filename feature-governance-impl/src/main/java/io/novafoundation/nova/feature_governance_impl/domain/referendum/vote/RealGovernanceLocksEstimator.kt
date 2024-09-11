package io.novafoundation.nova.feature_governance_impl.domain.referendum.vote

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AyeVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackInfo
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.asOngoingOrNull
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.flattenCastingVotes
import io.novafoundation.nova.feature_governance_api.domain.locks.RealClaimScheduleCalculator
import io.novafoundation.nova.feature_governance_api.domain.locks.reusable.LocksChange
import io.novafoundation.nova.feature_governance_api.domain.locks.reusable.ReusableLock
import io.novafoundation.nova.feature_governance_api.domain.locks.reusable.addIfPositive
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.Change
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.GovernanceVoteAssistant
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLock
import io.novafoundation.nova.feature_wallet_api.domain.model.maxLockReplacing
import io.novafoundation.nova.feature_wallet_api.domain.model.transferableReplacingFrozen
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import io.novafoundation.nova.runtime.util.BlockDurationEstimator

internal class RealGovernanceLocksEstimator(
    override val onChainReferendum: OnChainReferendum,
    private val voting: Map<TrackId, Voting>,
    private val votedReferenda: Map<ReferendumId, OnChainReferendum>,
    private val blockDurationEstimator: BlockDurationEstimator,
    private val tracks: Map<TrackId, TrackInfo>,
    private val votingLockId: String,
    undecidingTimeout: BlockNumber,
    voteLockingPeriod: BlockNumber,
    balanceLocks: List<BalanceLock>,
    governanceLocksByTrack: Map<TrackId, Balance>,
) : GovernanceVoteAssistant {

    private val claimScheduleCalculator = RealClaimScheduleCalculator(
        votingByTrack = voting,
        currentBlockNumber = blockDurationEstimator.currentBlock,
        // votedReferenda might not contain selected referenda so we add it manually
        referenda = votedReferenda + (onChainReferendum.id to onChainReferendum),
        tracks = tracks,
        undecidingTimeout = undecidingTimeout,
        voteLockingPeriod = voteLockingPeriod,
        trackLocks = governanceLocksByTrack,
    )

    private val flattenedVotes = voting.flattenCastingVotes()

    private val currentMaxGovernanceLocked = governanceLocksByTrack.values.maxOrNull().orZero()
    private val currentMaxUnlocksAt = estimateUnlocksAt(changedVote = null)

    private val otherMaxLocked = balanceLocks.maxLockReplacing(votingLockId, replaceWith = Balance.ZERO)

    private val allMaxLocked = balanceLocks.maxOfOrNull { it.amountInPlanks }
        .orZero()

    override val trackVoting: Voting? = voting.findVotingFor(onChainReferendum)

    override suspend fun estimateLocksAfterVoting(
        amount: Balance,
        conviction: Conviction,
        asset: Asset,
    ): LocksChange {
        val vote = AyeVote(amount, conviction) // vote direction does not influence lock estimation

        val newGovernanceLocked = currentMaxGovernanceLocked.max(amount)
        val newMaxUnlocksAt = estimateUnlocksAt(changedVote = vote)

        val previousLockDuration = blockDurationEstimator.durationUntil(currentMaxUnlocksAt)
        val newLockDuration = blockDurationEstimator.durationUntil(newMaxUnlocksAt)

        val currentTransferablePlanks = asset.transferableInPlanks
        val newLocked = otherMaxLocked.max(newGovernanceLocked)
        val newTransferablePlanks = asset.transferableReplacingFrozen(newLocked)

        return LocksChange(
            lockedAmountChange = Change(
                previousValue = currentMaxGovernanceLocked,
                newValue = newGovernanceLocked,
            ),
            lockedPeriodChange = Change(
                previousValue = previousLockDuration,
                newValue = newLockDuration,
            ),
            transferableChange = Change(
                previousValue = currentTransferablePlanks,
                newValue = newTransferablePlanks,
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
        val changedVoteMaxLock = claimScheduleCalculator.maxConvictionEndOf(changedVote, onChainReferendum.id)

        val currentVotesExceptChanged = votedReferenda.keys - onChainReferendum.id
        val currentVotesExceptChangedMaxUnlock = currentVotesExceptChanged.maxOfOrNull {
            val vote = flattenedVotes.getValue(it)

            claimScheduleCalculator.maxConvictionEndOf(vote, it)
        }.orZero()

        return changedVoteMaxLock.max(currentVotesExceptChangedMaxUnlock)
    }

    private fun votesEstimatedUnlocksAt(): BlockNumber {
        return flattenedVotes.maxOfOrNull { (referendumId, vote) ->
            claimScheduleCalculator.maxConvictionEndOf(vote, referendumId)
        }.orZero()
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
}
