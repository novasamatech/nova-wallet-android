package io.novafoundation.nova.feature_governance_impl.domain.referendum.common

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.common.utils.divideToDecimal
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendumStatus
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackInfo
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackQueue
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.asOngoing
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ayeVotes
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.inQueue
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.nayVotes
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.orEmpty
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.positionOf
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.supportThreshold
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.track
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSource
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumVoting
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumTimeline
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumTimeline.State
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.PreparingReason
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumStatus
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.util.BlockDurationEstimator
import io.novafoundation.nova.runtime.util.timerUntil
import java.math.BigInteger

interface ReferendaConstructor {

    fun constructReferendumVoting(
        referendum: OnChainReferendum,
        tracksById: Map<TrackId, TrackInfo>,
        currentBlockNumber: BlockNumber,
        totalIssuance: Balance,
    ): ReferendumVoting?

    suspend fun constructReferendaStatuses(
        chain: Chain,
        onChainReferenda: Collection<OnChainReferendum>,
        tracksById: Map<TrackId, TrackInfo>,
        currentBlockNumber: BlockNumber,
    ): Map<ReferendumId, ReferendumStatus>

    suspend fun constructPastTimeline(
        chain: Chain,
        onChainReferendum: OnChainReferendum,
        calculatedStatus: ReferendumStatus,
        currentBlockNumber: BlockNumber,
    ): List<ReferendumTimeline.Entry>
}

suspend fun ReferendaConstructor.constructReferendumStatus(
    chain: Chain,
    onChainReferendum: OnChainReferendum,
    tracksById: Map<TrackId, TrackInfo>,
    currentBlockNumber: BlockNumber,
): ReferendumStatus = constructReferendaStatuses(
    chain = chain,
    onChainReferenda = listOf(onChainReferendum),
    tracksById = tracksById,
    currentBlockNumber = currentBlockNumber,
).values.first()


class RealReferendaConstructor(
    private val governanceSourceRegistry: GovernanceSourceRegistry,
    private val chainStateRepository: ChainStateRepository,
) : ReferendaConstructor {

    override fun constructReferendumVoting(
        referendum: OnChainReferendum,
        tracksById: Map<TrackId, TrackInfo>,
        currentBlockNumber: BlockNumber,
        totalIssuance: Balance,
    ): ReferendumVoting? {
        val status = referendum.status

        if (status !is OnChainReferendumStatus.Ongoing) return null

        val track = tracksById.getValue(status.track)

        val elapsedSinceDecidingFraction = if (status.deciding != null) {
            val since = status.deciding!!.since
            val elapsed = (currentBlockNumber - since).coerceAtLeast(BigInteger.ZERO)

            elapsed.divideToDecimal(track.decisionPeriod)
        } else {
            Perbill.ZERO
        }

        return ReferendumVoting(
            support = ReferendumVoting.Support(
                threshold = track.supportThreshold(elapsedSinceDecidingFraction, totalIssuance),
                turnout = status.tally.support,
                electorate = totalIssuance
            ),
            approval = ReferendumVoting.Approval(
                ayeVotes = status.tally.ayeVotes(),
                nayVotes = status.tally.nayVotes(),
                threshold = track.minApproval.threshold(elapsedSinceDecidingFraction)
            )
        )
    }

    override suspend fun constructReferendaStatuses(
        chain: Chain,
        onChainReferenda: Collection<OnChainReferendum>,
        tracksById: Map<TrackId, TrackInfo>,
        currentBlockNumber: BlockNumber,
    ): Map<ReferendumId, ReferendumStatus> {
        val governanceSource = governanceSourceRegistry.sourceFor(chain.id)

        val blockTime = chainStateRepository.predictedBlockTime(chain.id)
        val blockDurationEstimator = BlockDurationEstimator(currentBlock = currentBlockNumber, blockTimeMillis = blockTime)

        val undecidingTimeout = governanceSource.referenda.undecidingTimeout(chain.id)

        val approvedReferendaExecutionBlocks = governanceSource.fetchExecutionBlocks(onChainReferenda, chain)
        val queuePositions = governanceSource.fetchQueuePositions(onChainReferenda, chain.id)

        return onChainReferenda.associateBy(
            keySelector = { it.id },
            valueTransform = { referendum ->
                when (val status = referendum.status) {
                    is OnChainReferendumStatus.Ongoing -> constructOngoingStatus(
                        status = status,
                        blockDurationEstimator = blockDurationEstimator,
                        undecidingTimeout = undecidingTimeout,
                        track = tracksById.getValue(status.track),
                        referendumId = referendum.id,
                        queuePositions = queuePositions,
                    )

                    OnChainReferendumStatus.Approved -> {
                        val executionBlock = approvedReferendaExecutionBlocks[referendum.id]

                        if (executionBlock != null) {
                            val executeIn = blockDurationEstimator.timerUntil(executionBlock)
                            ReferendumStatus.Approved(executeIn = executeIn)
                        } else {
                            ReferendumStatus.Executed
                        }
                    }

                    OnChainReferendumStatus.Cancelled -> ReferendumStatus.NotExecuted.Cancelled
                    OnChainReferendumStatus.Killed -> ReferendumStatus.NotExecuted.Killed
                    OnChainReferendumStatus.Rejected -> ReferendumStatus.NotExecuted.Rejected
                    OnChainReferendumStatus.TimedOut -> ReferendumStatus.NotExecuted.TimedOut
                }
            }
        )
    }

    override suspend fun constructPastTimeline(
        chain: Chain,
        onChainReferendum: OnChainReferendum,
        calculatedStatus: ReferendumStatus,
        currentBlockNumber: BlockNumber,
    ): List<ReferendumTimeline.Entry> {
        val blockTime = chainStateRepository.predictedBlockTime(chain.id)
        val blockDurationEstimator = BlockDurationEstimator(currentBlock = currentBlockNumber, blockTimeMillis = blockTime)

        fun MutableList<ReferendumTimeline.Entry>.add(state: State, at: BlockNumber?) {
            val entry = ReferendumTimeline.Entry(
                state = state,
                at = at?.let(blockDurationEstimator::timestampOf)
            )

            add(entry)
        }

        return buildList {
            when (calculatedStatus) {
                // for ongoing referenda, we have access to some timestamps on-chain
                is ReferendumStatus.Ongoing -> {
                    add(State.CREATED, at = onChainReferendum.status.asOngoing().submitted)
                }

                // for other kind of referenda, there is not historic timestamps on-chain
                ReferendumStatus.NotExecuted.Cancelled -> {
                    add(State.CREATED, at = null)
                    add(State.CANCELLED, at = null)
                }
                ReferendumStatus.NotExecuted.Killed -> {
                    add(State.CREATED, at = null)
                    add(State.KILLED, at = null)
                }
                ReferendumStatus.NotExecuted.Rejected -> {
                    add(State.CREATED, at = null)
                    add(State.REJECTED, at = null)
                }
                ReferendumStatus.NotExecuted.TimedOut -> {
                    add(State.CREATED, at = null)
                    add(State.TIMED_OUT, at = null)
                }
                is ReferendumStatus.Approved -> {
                    add(State.CREATED, at = null)
                    add(State.APPROVED, at = null)
                }
                ReferendumStatus.Executed -> {
                    add(State.CREATED, at = null)
                    add(State.APPROVED, at = null)
                    add(State.EXECUTED, at = null)
                }
            }
        }
    }

    private fun constructOngoingStatus(
        status: OnChainReferendumStatus.Ongoing,
        blockDurationEstimator: BlockDurationEstimator,
        undecidingTimeout: BlockNumber,
        track: TrackInfo,
        referendumId: ReferendumId,
        queuePositions: Map<ReferendumId, TrackQueue.Position>
    ): ReferendumStatus {
        return when {
            status.inQueue -> {
                val timeoutBlock = status.submitted + undecidingTimeout
                val timeOutIn = blockDurationEstimator.timerUntil(timeoutBlock)
                val positionInQueue = queuePositions.getValue(referendumId)

                ReferendumStatus.Ongoing.InQueue(timeOutIn, positionInQueue)
            }

            // confirming
            status.deciding?.confirming != null -> {
                val approveBlock = status.deciding!!.confirming!!.till
                val approveIn = blockDurationEstimator.timerUntil(approveBlock)

                ReferendumStatus.Ongoing.Confirming(approveIn = approveIn)
            }

            // rejecting
            status.deciding != null -> {
                val rejectBlock = status.deciding!!.since + track.decisionPeriod
                val rejectIn = blockDurationEstimator.timerUntil(rejectBlock)

                ReferendumStatus.Ongoing.Rejecting(rejectIn)
            }

            // preparing
            else -> {
                if (status.decisionDeposit != null) {
                    val preparedBlock = status.submitted + track.preparePeriod
                    val preparedIn = blockDurationEstimator.timerUntil(preparedBlock)

                    ReferendumStatus.Ongoing.Preparing(reason = PreparingReason.DecidingIn(preparedIn))
                } else {
                    ReferendumStatus.Ongoing.Preparing(reason = PreparingReason.WaitingForDeposit)
                }
            }
        }
    }

    private suspend fun GovernanceSource.fetchExecutionBlocks(
        onChainReferenda: Collection<OnChainReferendum>,
        chain: Chain
    ): Map<ReferendumId, BlockNumber> {
        val approvedReferendaIds = onChainReferenda.mapNotNull { referendum ->
            referendum.id.takeIf { referendum.status is OnChainReferendumStatus.Approved }
        }
        return referenda.getReferendaExecutionBlocks(chain.id, approvedReferendaIds)
    }

    private suspend fun GovernanceSource.fetchQueuePositions(
        onChainReferenda: Collection<OnChainReferendum>,
        chainId: ChainId,
    ): Map<ReferendumId, TrackQueue.Position> {
        val queuedReferenda = onChainReferenda.filter { it.status.inQueue() }
        val tracksIdsToFetchQueues = queuedReferenda.mapNotNullTo(mutableSetOf(), OnChainReferendum::track)

        val queues = referenda.getTrackQueues(tracksIdsToFetchQueues, chainId)

        return queuedReferenda.associateBy(
            keySelector = OnChainReferendum::id,
            valueTransform = {
                val track = it.track()!! // safe since referendum is ongoing
                val queue = queues[track].orEmpty()

                queue.positionOf(it.id)
            }
        )
    }
}
