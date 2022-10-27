package io.novafoundation.nova.feature_governance_api.domain.locks

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AyeVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendumStatus
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.PriorLock
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.domain.locks.ClaimSchedule.ClaimAction
import io.novafoundation.nova.feature_governance_api.domain.locks.ClaimSchedule.UnlockChunk
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction

interface ClaimScheduleTestBuilder {

    fun given(builder: Given.() -> Unit)

    interface Given {

        fun currentBlock(block: Int)

        fun track(trackId: Int, builder: Track.() -> Unit)

        interface Track {

            fun lock(lock: Int)

            fun voting(builder: Voting.() -> Unit)

            interface Voting {

                fun prior(amount: Int, unlockAt: Int)

                fun vote(amount: Int, referendumId: Int, unlockAt: Int)
            }

        }
    }

    fun expect(builder: Expect.() -> Unit)

    interface Expect {

        fun claimable(amount: Int, actions: ClaimableActions.() -> Unit)

        fun nonClaimable(amount: Int, claimAt: Int)

        interface ClaimableActions {

            fun unlock(trackId: Int)

            fun removeVote(trackId: Int, referendumId: Int)
        }
    }
}

fun ClaimScheduleTest(builder: ClaimScheduleTestBuilder.() -> Unit) {
    val test = ClaimScheduleTest().apply(builder)

    test.runTest()
}

private class ClaimScheduleTest : ClaimScheduleTestBuilder {

    private var calculator: RealClaimScheduleCalculator? = null
    private var expectedSchedule: ClaimSchedule? = null

    override fun given(builder: ClaimScheduleTestBuilder.Given.() -> Unit) {
        calculator = GivenBuilder().apply(builder).build()
    }

    override fun expect(builder: ClaimScheduleTestBuilder.Expect.() -> Unit) {
        expectedSchedule = ExpectedBuilder().apply(builder).buildSchedule()
    }

    fun runTest() {
        val actualSchedule = calculator!!.estimateClaimSchedule()

        assert(actualSchedule == expectedSchedule!!) {
            buildString {
                append("Expected schedule: $expectedSchedule\n")
                append("Actual schedule  : $actualSchedule\n")
            }
        }
    }
}

private class ExpectedBuilder : ClaimScheduleTestBuilder.Expect {

    private var chunks = mutableListOf<UnlockChunk>()

    override fun claimable(amount: Int, actionsBuilder: ClaimScheduleTestBuilder.Expect.ClaimableActions.() -> Unit) {
        val actions = ClaimableActionsBuilder().apply(actionsBuilder).buildActions()

        chunks.add(UnlockChunk.Claimable(amount.toBigInteger(), actions))
    }

    override fun nonClaimable(amount: Int, claimAt: Int) {
        chunks.add(UnlockChunk.Pending(amount.toBigInteger(), claimAt.toBigInteger()))
    }

    fun buildSchedule(): ClaimSchedule {
        return ClaimSchedule(chunks)
    }
}

private class ClaimableActionsBuilder : ClaimScheduleTestBuilder.Expect.ClaimableActions {

    private val actions = mutableListOf<ClaimAction>()


    override fun unlock(trackId: Int) {
        val action = ClaimAction.Unlock(TrackId(trackId.toBigInteger()))
        actions.add(action)
    }

    override fun removeVote(trackId: Int, referendumId: Int) {
        val trackIdTyped = TrackId(trackId.toBigInteger())
        val referendumIdTyped = ReferendumId(referendumId.toBigInteger())

        val action = ClaimAction.RemoveVote(trackIdTyped, referendumIdTyped)
        actions.add(action)
    }

    fun buildActions(): List<ClaimAction> {
        return actions
    }
}

private class GivenBuilder : ClaimScheduleTestBuilder.Given {

    private var voting: MutableMap<TrackId, Voting> = mutableMapOf()
    private var currentBlockNumber: BlockNumber = BlockNumber.ZERO
    private var referenda: MutableMap<ReferendumId, OnChainReferendum> = mutableMapOf()
    private var trackLocks: MutableMap<TrackId, Balance> = mutableMapOf()

    override fun currentBlock(block: Int) {
        currentBlockNumber = block.toBigInteger()
    }

    override fun track(trackId: Int, builder: ClaimScheduleTestBuilder.Given.Track.() -> Unit) {
        val trackIdTyped = TrackId(trackId.toBigInteger())
        val builtTrack = TrackBuilder().apply(builder)

        voting[trackIdTyped] = builtTrack.buildVoting()

        val newReferenda = builtTrack.buildReferendaApprovedAt().mapValues { (referendaId, approvedAt) ->
            OnChainReferendum(
                id = referendaId,
                status = OnChainReferendumStatus.Approved(since = approvedAt)
            )
        }
        referenda += newReferenda

        trackLocks[trackIdTyped] = builtTrack.buildTrackLock()
    }

    fun build(): RealClaimScheduleCalculator {
        return RealClaimScheduleCalculator(
            voting = voting,
            currentBlockNumber = currentBlockNumber,
            referenda = referenda,
            trackLocks = trackLocks,

            // those parameters are only used for ongoing referenda estimation
            // we only use approved ones in this tests
            tracks = emptyMap(),
            undecidingTimeout = BlockNumber.ZERO,

            // we do not use conviction in tests
            voteLockingPeriod = BlockNumber.ZERO
        )
    }

}

private fun PriorLock(): PriorLock = PriorLock(BlockNumber.ZERO, Balance.ZERO)

private class TrackBuilder : ClaimScheduleTestBuilder.Given.Track, ClaimScheduleTestBuilder.Given.Track.Voting {

    private var trackLock: Balance = Balance.ZERO

    private var prior: PriorLock = PriorLock()
    private val votes = mutableMapOf<ReferendumId, AccountVote>()
    private var referendumApprovedAt = mutableMapOf<ReferendumId, BlockNumber>()

    override fun lock(lock: Int) {
        trackLock = lock.toBigInteger()
    }

    override fun voting(builder: ClaimScheduleTestBuilder.Given.Track.Voting.() -> Unit) {
        builder(this)
    }

    override fun prior(amount: Int, unlockAt: Int) {
        prior = PriorLock(unlockAt = unlockAt.toBigInteger(), amount = amount.toBigInteger())
    }

    override fun vote(amount: Int, referendumId: Int, unlockAt: Int) {
        val referendumIdTyped = ReferendumId(referendumId.toBigInteger())
        votes[referendumIdTyped] = AyeVote(amount.toBigInteger(), Conviction.None)
        referendumApprovedAt[referendumIdTyped] = unlockAt.toBigInteger()
    }

    fun buildVoting(): Voting.Casting {
        return Voting.Casting(votes, prior)
    }

    fun buildReferendaApprovedAt(): Map<ReferendumId, BlockNumber> {
        return referendumApprovedAt
    }

    fun buildTrackLock(): Balance {
        return trackLock
    }
}
