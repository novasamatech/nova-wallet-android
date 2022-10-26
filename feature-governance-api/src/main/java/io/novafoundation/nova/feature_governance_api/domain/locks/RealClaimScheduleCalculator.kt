package io.novafoundation.nova.feature_governance_api.domain.locks

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.utils.castOrNull
import io.novafoundation.nova.common.utils.mapNotNullToSet
import io.novafoundation.nova.common.utils.mapValuesNotNull
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendumStatus
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackInfo
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.completedReferendumLockDuration
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.maxLockDuration
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.totalLock
import io.novafoundation.nova.feature_governance_api.domain.locks.ClaimSchedule.ClaimAction
import io.novafoundation.nova.feature_governance_api.domain.locks.ClaimSchedule.ClaimAction.RemoveVote
import io.novafoundation.nova.feature_governance_api.domain.locks.ClaimSchedule.ClaimAction.Unlock
import io.novafoundation.nova.feature_governance_api.domain.locks.ClaimSchedule.UnlockChunk
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import jp.co.soramitsu.fearless_utils.hash.isPositive

private data class ClaimableLock(
    val claimAt: BlockNumber,
    val amount: Balance,
    val affected: Set<ClaimAffect>,
)

private sealed class ClaimAffect(open val trackId: TrackId) {

    data class Prior(override val trackId: TrackId) : ClaimAffect(trackId)

    data class Vote(override val trackId: TrackId, val referendumId: ReferendumId) : ClaimAffect(trackId)
}

private class GroupedClaimAffects(
    val trackId: TrackId,
    val hasPriorAffect: Boolean,
    val votes: List<ClaimAffect.Vote>
)

private class UnlockGap(
    val balance: Balance,
    val tracks: Set<TrackId>
)

class RealClaimScheduleCalculator(
    private val voting: Map<TrackId, Voting>,
    private val currentBlockNumber: BlockNumber,
    private val referenda: Map<ReferendumId, OnChainReferendum>,
    private val tracks: Map<TrackId, TrackInfo>,
    private val undecidingTimeout: BlockNumber,
    private val voteLockingPeriod: BlockNumber,
    private val trackLocks: Map<TrackId, Balance>,
) : ClaimScheduleCalculator {

    override fun maxConvictionEndOf(vote: AccountVote, referendumId: ReferendumId): BlockNumber {
        return referenda.getValue(referendumId).maxConvictionEnd(vote)
    }

    override fun estimateClaimSchedule(): ClaimSchedule {
        val castingVotes = voting.entries.mapNotNull { (trackId, voting) ->
            voting.castOrNull<Voting.Casting>()?.let { trackId to it }
        }

        // step 1 - determine/estimate individual unlocks for all priors and votes
        // result example: [(1500, 1 KSM), (1200, 2 KSM), (1000, 1 KSM)]
        val claimableLocks = individualClaimableLocks(castingVotes)

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

    private fun individualClaimableLocks(castingVotes: List<Pair<TrackId, Voting.Casting>>): List<ClaimableLock> {
        return castingVotes.flatMap { (trackId, voting) ->
            val priorLock = ClaimableLock(
                claimAt = voting.prior.unlockAt,
                amount = voting.prior.amount,
                affected = setOf(ClaimAffect.Prior(trackId))
            )

            val standardVotes = voting.votes.mapValuesNotNull { (_, votes) ->
                votes.castOrNull<AccountVote.Standard>()
            }

            val standardVoteLocks = standardVotes.map { (referendumId, standardVote) ->
                val estimatedEnd = maxConvictionEndOf(standardVote, referendumId)
                val lock = ClaimableLock(
                    claimAt = estimatedEnd,
                    amount = standardVote.balance,
                    affected = setOf(ClaimAffect.Vote(trackId, referendumId))
                )

                // we estimate whether prior will affect the vote when performing `removeVote`
                lock.timeAtLeast(priorLock.claimAt)
            }

            buildList {
                if (priorLock.reasonableToClaim()) add(priorLock)

                addAll(standardVoteLocks)
            }
        }
    }

    private fun combineSameUnlockAt(claimableLocks: List<ClaimableLock>) =
        claimableLocks.groupBy(ClaimableLock::claimAt)
            .mapValues { (_, locks) ->
                locks.reduce { current, next -> current.foldSameTime(next) }
            }

    private fun constructUnlockSchedule(maxUnlockedByTime: Map<BlockNumber, ClaimableLock>): List<ClaimableLock> {
        var currentMaxLock = Balance.ZERO
        var currentMaxLockAt: BlockNumber? = null

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
                    // this lock is completely shadowed by later (in time) locks with greater values
                    result.remove(at)

                    // but we want to keep its actions so we move it to the current known maximum that goes later in time
                    result.computeIfPresent(currentMaxLockAt!!) { _, maxLock ->
                        maxLock.copy(affected = maxLock.affected + lock.affected)
                    }
                }
            }

        return result.toSortedMap(reverseOrder())
            .values.toList()
    }

    @Suppress("UNCHECKED_CAST")
    private fun List<ClaimableLock>.toUnlockChunks(): List<UnlockChunk> {
        val chunks = map { it.toUnlockChunk(currentBlockNumber) }
        val (claimable, nonClaimable) = chunks.partition { it is UnlockChunk.Claimable }

        // fold all claimable chunks to single one
        val initialClaimable = UnlockChunk.Claimable(amount = Balance.ZERO, actions = emptyList())
        var claimableChunk = (claimable as List<UnlockChunk.Claimable>).fold(initialClaimable) { acc, unlockChunk ->
            UnlockChunk.Claimable(acc.amount + unlockChunk.amount, acc.actions + unlockChunk.actions)
        }

        // handle gap between tracks locked and votes
        val gapBetweenVotingAndLocked = voting.gapWith(trackLocks)
        if (gapBetweenVotingAndLocked.balance.isPositive()) {
            val tracksAlreadyToUnlock = claimableChunk.actions.mapNotNullToSet { it.castOrNull<Unlock>()?.trackId }
            val additionalTracksToUnlock = gapBetweenVotingAndLocked.tracks - tracksAlreadyToUnlock
            val additionalUnlockActions = additionalTracksToUnlock.map(ClaimAction::Unlock)

            val totalClaimable = claimableChunk.amount + gapBetweenVotingAndLocked.balance
            val totalClaimableActions = additionalUnlockActions + claimableChunk.actions

            claimableChunk = UnlockChunk.Claimable(amount = totalClaimable, actions = totalClaimableActions)
        }

        return buildList {
            if (claimableChunk.amount.isPositive()) {
                add(claimableChunk)
            }

            addAll(nonClaimable)
        }
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
                val approveBlock = deciding.confirming.till
                val rejectBlock = deciding.since + decisionPeriod

                approveBlock.max(rejectBlock)
            }

            // rejecting
            deciding != null -> {
                val rejectBlock = deciding.since + decisionPeriod

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

private fun ClaimableLock.foldSameTime(another: ClaimableLock): ClaimableLock {
    require(claimAt == another.claimAt)

    return ClaimableLock(
        claimAt = claimAt,
        amount = another.amount.max(another.amount),
        affected = affected + another.affected
    )
}

private fun ClaimableLock.reasonableToClaim(): Boolean {
    return amount > Balance.ZERO
}

private infix fun ClaimableLock.timeAtLeast(time: BlockNumber): ClaimableLock {
    return copy(claimAt = claimAt.max(time))
}

private fun ClaimableLock.claimableAt(at: BlockNumber): Boolean {
    return claimAt <= at
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

    return UnlockGap(
        balance = gapByTrack.values.maxOrNull().orZero(),
        tracks = gapByTrack.entries.mapNotNullToSet { (trackId, gap) -> trackId.takeIf { gap.isPositive() } }
    )
}

private fun Collection<ClaimAffect>.toClaimActions(): List<ClaimAction> {
    return groupByTrack().flatMap { trackAffects ->
        buildList {
            if (trackAffects.hasPriorAffect) {
                add(Unlock(trackAffects.trackId))
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
            hasPriorAffect = trackAffects.any { it is ClaimAffect.Prior },
            votes = trackAffects.filterIsInstance<ClaimAffect.Vote>()
        )
    }
}
