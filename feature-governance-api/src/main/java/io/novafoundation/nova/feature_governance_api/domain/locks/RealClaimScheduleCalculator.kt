package io.novafoundation.nova.feature_governance_api.domain.locks

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ConfirmingSource
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendumStatus
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackInfo
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.amount
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.completedReferendumLockDuration
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.maxLockDuration
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.totalLock
import io.novafoundation.nova.feature_governance_api.domain.locks.ClaimSchedule.ClaimAction
import io.novafoundation.nova.feature_governance_api.domain.locks.ClaimSchedule.ClaimAction.RemoveVote
import io.novafoundation.nova.feature_governance_api.domain.locks.ClaimSchedule.ClaimAction.Unlock
import io.novafoundation.nova.feature_governance_api.domain.locks.ClaimSchedule.UnlockChunk
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novasama.substrate_sdk_android.hash.isPositive

private data class ClaimableLock(
    val claimAt: ClaimTime,
    val amount: Balance,
    val affected: Set<ClaimAffect>,
)

private sealed class ClaimAffect(open val trackId: TrackId) {

    data class Track(override val trackId: TrackId) : ClaimAffect(trackId)

    data class Vote(override val trackId: TrackId, val referendumId: ReferendumId) : ClaimAffect(trackId)
}

private class GroupedClaimAffects(
    val trackId: TrackId,
    val hasPriorAffect: Boolean,
    val votes: List<ClaimAffect.Vote>
)

typealias UnlockGap = Map<TrackId, Balance>

class RealClaimScheduleCalculator(
    private val votingByTrack: Map<TrackId, Voting>,
    private val currentBlockNumber: BlockNumber,
    private val referenda: Map<ReferendumId, OnChainReferendum>,
    private val tracks: Map<TrackId, TrackInfo>,
    private val undecidingTimeout: BlockNumber,
    private val voteLockingPeriod: BlockNumber,
    private val trackLocks: Map<TrackId, Balance>,
) : ClaimScheduleCalculator {

    override fun totalGovernanceLock(): Balance {
        return trackLocks.values.maxOrNull().orZero()
    }

    @Suppress("IfThenToElvis")
    override fun maxConvictionEndOf(vote: AccountVote, referendumId: ReferendumId): BlockNumber {
        val referendum = referenda[referendumId]

        return if (referendum != null) {
            referendum.maxConvictionEnd(vote)
        } else {
            // referendum is not in the map, which means it is cancelled and votes can be unlocked immediately
            currentBlockNumber
        }
    }

    /**
     * Given the information about Voting (priors + active votes), statuses of referenda and TrackLocks
     * Constructs the estimated claiming schedule.
     * The schedule is exact when all involved referenda are completed. Only ongoing referenda' end time is estimateted
     *
     * The claiming schedule shows how much tokens will be unlocked and when.
     * Schedule may consist of zero or one [ClaimSchedule.UnlockChunk.Claimable] chunk
     * and zero or more [ClaimSchedule.UnlockChunk.Pending] chunks.
     *
     * [ClaimSchedule.UnlockChunk.Pending] chunks also provides a set of [ClaimSchedule.ClaimAction] actions
     * needed to claim whole chunk.
     *
     * The algorithm itself consists of several parts
     *
     * 1. Determine individual unlocks
     * This step is based on prior [Voting.Casting.prior] and [AccountVote.Standard] standard votes
     * a. Each non-zero prior has a single individual unlock
     * b. Each non-zero vote has a single individual unlock.
     *    However, unlock time for votes is at least unlock time of corresponding prior.
     * c. Find a gap between [votingByTrack] and [trackLocks], which indicates an extra claimable amount
     *    To provide additive effect of gap, we add total voting lock on top of it:
     if [votingByTrack] has some pending locks - they gonna delay their amount but always leaving trackGap untouched & claimable
     On the other hand, if other tracks have locks bigger than [votingByTrack]'s total lock,
     trackGap will be partially or full delayed by them
     *
     * During this step we also determine the list of [ClaimAffect],
     * which later gets translated to [ClaimSchedule.ClaimAction].
     *
     * 2. Combine all locks with the same unlock time into single lock
     *  a. Result's amount is the maximum between combined locks
     *  b. Result's affects is a concatenation of all affects from combined locks
     *
     * 3. Construct preliminary unlock schedule based on the following algorithm
     *   a. Sort pairs from step (2) by descending [ClaimableLock.claimAt] order
     *   b. For each item in the sorted list, find the difference between the biggest currently processed lock and item's amount
     *   c. Since we start from the most far locks in the future, finding a positive difference means that
     *   this difference is actually an entry in desired unlock schedule. Negative difference means that this unlock is
     *   completely covered by future's unlock with bigger amount. Thus, we should discard it from the schedule and move its affects
     *   to the currently known maximum lock in order to not to loose its actions when unlocking maximum lock.
     *
     * 4. Check which if unlocks are claimable and which are not by constructing [ClaimSchedule.UnlockChunk] based on [currentBlockNumber]
     * 5. Fold all [ClaimSchedule.UnlockChunk] into single chunk.
     * 6. If gap exists, then we should add it to claimable chunk. We should also check if we should perform extra [ClaimSchedule.ClaimAction.Unlock]
     *  for each track that is included in the gap. We do that by finding by checking which [ClaimSchedule.ClaimAction.Unlock] unlocks are already present
     *  in claimable chunk's actions in order to not to do them twice.
     */
    override fun estimateClaimSchedule(): ClaimSchedule {
        // step 1 - determine/estimate individual unlocks for all priors and votes
        // result example: [(1500, 1 KSM), (1200, 2 KSM), (1000, 1 KSM)]
        val claimableLocks = individualClaimableLocks()

        // step 2 - fold all locks with same lockAt
        // { 1500: 1 KSM, 1200: 2 KSM, 1000: 1 KSM }
        val maxUnlockedByTime = combineSameUnlockAt(claimableLocks)

        // step 3 - convert individual schedule to global
        // [(1500, 1 KSM), (1200, 1 KSM)]
        val unlockSchedule = constructUnlockSchedule(maxUnlockedByTime)

        // step 4 - convert locks affects to claim actions
        val chunks = unlockSchedule.toUnlockChunks()

        return ClaimSchedule(chunks)
    }

    private fun individualClaimableLocks(): List<ClaimableLock> {
        val gapBetweenVotingAndLocked = votingByTrack.gapWith(trackLocks)

        return votingByTrack.flatMap { (trackId, voting) ->
            buildList {
                gapClaimableLock(trackId, voting, gapBetweenVotingAndLocked)

                when (voting) {
                    is Voting.Casting -> castingClaimableLocks(trackId, voting)
                    is Voting.Delegating -> delegatingClaimableLocks(trackId, voting)
                }
            }
        }
    }

    private fun MutableList<ClaimableLock>.gapClaimableLock(trackId: TrackId, voting: Voting, gap: UnlockGap) {
        val trackGap = gap[trackId].orZero()

        if (trackGap.isPositive()) {
            val lock = ClaimableLock(
                claimAt = ClaimTime.At(currentBlockNumber),
                amount = trackGap + voting.totalLock(),
                affected = setOf(ClaimAffect.Track(trackId))
            )

            add(lock)
        }
    }

    private fun MutableList<ClaimableLock>.delegatingClaimableLocks(trackId: TrackId, voting: Voting.Delegating) {
        val delegationLock = ClaimableLock(
            claimAt = ClaimTime.UntilAction,
            amount = voting.amount,
            affected = emptySet()
        )
        val priorLock = ClaimableLock(
            claimAt = ClaimTime.At(voting.prior.unlockAt),
            amount = voting.prior.amount,
            affected = setOf(ClaimAffect.Track(trackId))
        )

        add(delegationLock)
        if (priorLock.reasonableToClaim()) add(priorLock)
    }

    private fun MutableList<ClaimableLock>.castingClaimableLocks(trackId: TrackId, voting: Voting.Casting) {
        val priorLock = ClaimableLock(
            claimAt = ClaimTime.At(voting.prior.unlockAt),
            amount = voting.prior.amount,
            affected = setOf(ClaimAffect.Track(trackId))
        )

        val standardVoteLocks = voting.votes.map { (referendumId, standardVote) ->
            val estimatedEnd = maxConvictionEndOf(standardVote, referendumId)
            val lock = ClaimableLock(
                claimAt = ClaimTime.At(estimatedEnd),
                amount = standardVote.amount(),
                affected = setOf(ClaimAffect.Vote(trackId, referendumId))
            )

            // we estimate whether prior will affect the vote when performing `removeVote`
            lock.timeAtLeast(priorLock.claimAt)
        }

        if (priorLock.reasonableToClaim()) add(priorLock)
        addAll(standardVoteLocks)
    }

    private fun combineSameUnlockAt(claimableLocks: List<ClaimableLock>) =
        claimableLocks.groupBy(ClaimableLock::claimAt)
            .mapValues { (_, locks) ->
                locks.reduce { current, next -> current.foldSameTime(next) }
            }

    private fun constructUnlockSchedule(maxUnlockedByTime: Map<ClaimTime, ClaimableLock>): List<ClaimableLock> {
        var currentMaxLock = Balance.ZERO
        var currentMaxLockAt: ClaimTime? = null

        val result = maxUnlockedByTime.toMutableMap()

        maxUnlockedByTime.entries.sortedByDescending { it.key }
            .forEach { (at, lock) ->
                val newMaxLock = currentMaxLock.max(lock.amount)
                val unlockedAmount = lock.amount - currentMaxLock

                val shouldSetNewMax = currentMaxLockAt == null || currentMaxLock < newMaxLock
                if (shouldSetNewMax) {
                    currentMaxLock = newMaxLock
                    currentMaxLockAt = at
                }

                if (unlockedAmount.isPositive()) {
                    // there is something to unlock at this point
                    result[at] = lock.copy(amount = unlockedAmount)
                } else {
                    // this lock is completely shadowed by later (in time) lock with greater value
                    result.remove(at)

                    // but we want to keep its actions so we move it to the current known maximum that goes later in time
                    result.computeIfPresent(currentMaxLockAt!!) { _, maxLock ->
                        maxLock.copy(affected = maxLock.affected + lock.affected)
                    }
                }
            }

        return result.toSortedMap().values.toList()
    }

    @Suppress("UNCHECKED_CAST")
    private fun List<ClaimableLock>.toUnlockChunks(): List<UnlockChunk> {
        val chunks = map { it.toUnlockChunk(currentBlockNumber) }
        val (claimable, nonClaimable) = chunks.partition { it is UnlockChunk.Claimable }

        // fold all claimable chunks to single one
        val initialClaimable = Balance.ZERO to emptyList<ClaimAction>()

        val (claimableAmount, claimableActions) = (claimable as List<UnlockChunk.Claimable>).fold(initialClaimable) { (amount, actions), unlockChunk ->
            val nextAmount = amount + unlockChunk.amount
            val nextActions = actions + unlockChunk.actions
            nextAmount to nextActions
        }
        val claimableChunk = constructClaimableChunk(claimableAmount, claimableActions)

        return buildList {
            if (claimableChunk.amount.isPositive()) {
                add(claimableChunk)
            }

            addAll(nonClaimable)
        }
    }

    private fun constructClaimableChunk(
        claimableAmount: Balance,
        claimableActions: List<ClaimAction>
    ): UnlockChunk.Claimable {
        return UnlockChunk.Claimable(claimableAmount, claimableActions.dedublicateUnlocks())
    }

    // We want to avoid doing multiple unlocks for the same track
    // For that we also need to move unlock() calls to the end
    private fun List<ClaimAction>.dedublicateUnlocks(): List<ClaimAction> {
        return distinct().sortedBy { it is Unlock }
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

            deciding != null -> {
                when (val source = deciding.confirming) {
                    is ConfirmingSource.FromThreshold -> source.end

                    is ConfirmingSource.OnChain -> if (source.status != null) {
                        // confirming
                        val approveBlock = source.status.till
                        val rejectBlock = deciding.since + decisionPeriod

                        approveBlock.max(rejectBlock)
                    } else {
                        // rejecting
                        val rejectBlock = deciding.since + decisionPeriod

                        rejectBlock
                    }
                }
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

private fun ClaimableLock.foldSameTime(another: ClaimableLock): ClaimableLock {
    require(claimAt == another.claimAt)

    return ClaimableLock(
        claimAt = claimAt,
        amount = amount.max(another.amount),
        affected = affected + another.affected
    )
}

private fun ClaimableLock.reasonableToClaim(): Boolean {
    return amount.isPositive()
}

private infix fun ClaimableLock.timeAtLeast(time: ClaimTime): ClaimableLock {
    val newClaimAt = maxOf(claimAt, time)

    return copy(claimAt = newClaimAt)
}

private fun ClaimableLock.claimableAt(at: BlockNumber): Boolean {
    return when (claimAt) {
        is ClaimTime.At -> claimAt.block <= at
        ClaimTime.UntilAction -> false
    }
}

private fun ClaimableLock.toUnlockChunk(currentBlockNumber: BlockNumber): UnlockChunk {
    return if (claimableAt(currentBlockNumber)) {
        UnlockChunk.Claimable(
            amount = amount,
            actions = affected.toClaimActions()
        )
    } else {
        UnlockChunk.Pending(amount, claimAt)
    }
}

private fun Map<TrackId, Voting>.gapWith(locksByTrackId: Map<TrackId, Balance>): UnlockGap {
    val gapByTrack = mapValues { (trackId, voting) ->
        val trackLock = locksByTrackId[trackId].orZero()
        val gap = (trackLock - voting.totalLock()).coerceAtLeast(Balance.ZERO)

        gap
    }

    return gapByTrack
}

private fun Collection<ClaimAffect>.toClaimActions(): List<ClaimAction> {
    return groupByTrack().flatMap { trackAffects ->
        buildList {
            if (trackAffects.hasPriorAffect) {
                val requiresStandaloneUnlock = trackAffects.votes.isEmpty()

                if (requiresStandaloneUnlock) {
                    add(Unlock(trackAffects.trackId))
                }
            }

            if (trackAffects.votes.isNotEmpty()) {
                trackAffects.votes.forEach { voteAffect ->
                    add(RemoveVote(voteAffect.trackId, voteAffect.referendumId))
                }

                add(Unlock(trackAffects.votes.first().trackId))
            }
        }
    }
}

private fun Collection<ClaimAffect>.groupByTrack(): List<GroupedClaimAffects> {
    return groupBy(ClaimAffect::trackId).entries.map { (trackId, trackAffects) ->
        GroupedClaimAffects(
            trackId = trackId,
            hasPriorAffect = trackAffects.any { it is ClaimAffect.Track },
            votes = trackAffects.filterIsInstance<ClaimAffect.Vote>()
        )
    }
}
