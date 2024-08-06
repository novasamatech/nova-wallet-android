package io.novafoundation.nova.feature_governance_impl.domain.referendum.common

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.asLoaded
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.utils.divideToDecimal
import io.novafoundation.nova.common.utils.orFalse
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ConfirmingSource
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendumStatus
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Tally
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackInfo
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackQueue
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.asOngoing
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ayeVotes
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.inQueue
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.nayVotes
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.orEmpty
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.positionOf
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.sinceOrThrow
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.till
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.tillOrNull
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.track
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.OffChainReferendumVotingDetails
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.getAbstain
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.referendum.toTallyOrNull
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSource
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumThreshold
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.ReferendumVoting
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.currentlyPassing
import io.novafoundation.nova.feature_governance_api.domain.referendum.common.projectedPassing
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
        tally: Tally?,
        currentBlockNumber: BlockNumber,
        electorate: Balance?,
        offChainVotingDetails: ExtendedLoadingState<OffChainReferendumVotingDetails?>
    ): ReferendumVoting

    fun constructReferendumThreshold(
        referendum: OnChainReferendum,
        tracksById: Map<TrackId, TrackInfo>?,
        currentBlockNumber: BlockNumber,
        electorate: Balance?
    ): ReferendumThreshold?

    suspend fun constructReferendaStatuses(
        selectedGovernanceOption: SupportedGovernanceOption,
        onChainReferenda: Collection<OnChainReferendum>,
        votingByReferenda: Map<ReferendumId, ReferendumVoting?>,
        thresholdByReferenda: Map<ReferendumId, ReferendumThreshold?>,
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
    selectedGovernanceOption: SupportedGovernanceOption,
    onChainReferendum: OnChainReferendum,
    tracksById: Map<TrackId, TrackInfo>,
    votingByReferenda: Map<ReferendumId, ReferendumVoting?>,
    thresholdByReferenda: Map<ReferendumId, ReferendumThreshold?>,
    currentBlockNumber: BlockNumber,
): ReferendumStatus = constructReferendaStatuses(
    selectedGovernanceOption = selectedGovernanceOption,
    onChainReferenda = listOf(onChainReferendum),
    tracksById = tracksById,
    currentBlockNumber = currentBlockNumber,
    votingByReferenda = votingByReferenda,
    thresholdByReferenda = thresholdByReferenda
).values.first()

class RealReferendaConstructor(
    private val governanceSourceRegistry: GovernanceSourceRegistry,
    private val chainStateRepository: ChainStateRepository,
) : ReferendaConstructor {

    override fun constructReferendumVoting(
        tally: Tally?,
        currentBlockNumber: BlockNumber,
        electorate: Balance?,
        offChainVotingDetails: ExtendedLoadingState<OffChainReferendumVotingDetails?>
    ): ReferendumVoting {
        return ReferendumVoting(
            support = votingLoadingState(tally, electorate, offChainVotingDetails) { _tally, _electorate ->
                ReferendumVoting.Support(
                    turnout = _tally.support,
                    electorate = _electorate
                )
            },
            approval = votingLoadingState(tally, electorate, offChainVotingDetails) { _tally, _ ->
                ReferendumVoting.Approval(
                    ayeVotes = _tally.ayeVotes(),
                    nayVotes = _tally.nayVotes(),
                )
            },
            abstainVotes = offChainVotingDetails.map { it?.votingInfo?.getAbstain()?.toBigInteger() }
        )
    }

    private fun <T> votingLoadingState(
        onChainTally: Tally?,
        electorate: Balance?,
        offChainVotingDetails: ExtendedLoadingState<OffChainReferendumVotingDetails?>,
        onLoaded: (Tally, Balance) -> T
    ): ExtendedLoadingState<T?> {
        val tallyOrNull = onChainTally ?: offChainVotingDetails.dataOrNull?.votingInfo?.toTallyOrNull()
        return when {
            tallyOrNull != null && electorate != null -> onLoaded(tallyOrNull, electorate).asLoaded()
            offChainVotingDetails.isLoading() || electorate == null -> ExtendedLoadingState.Loading
            else -> ExtendedLoadingState.Loaded(null)
        }
    }

    override fun constructReferendumThreshold(
        referendum: OnChainReferendum,
        tracksById: Map<TrackId, TrackInfo>?,
        currentBlockNumber: BlockNumber,
        electorate: Balance?
    ): ReferendumThreshold? {
        val status = referendum.status
        if (status !is OnChainReferendumStatus.Ongoing) return null
        if (tracksById == null || electorate == null) return null

        val track = tracksById.getValue(status.track)

        val elapsedSinceDecidingFraction = if (status.deciding != null) {
            val since = status.deciding!!.since
            val elapsed = (currentBlockNumber - since).coerceAtLeast(BigInteger.ZERO)

            elapsed.divideToDecimal(track.decisionPeriod)
        } else {
            Perbill.ZERO
        }

        return ReferendumThreshold(
            support = status.threshold.supportThreshold(status.tally, electorate, elapsedSinceDecidingFraction),
            approval = status.threshold.ayesFractionThreshold(status.tally, electorate, elapsedSinceDecidingFraction)
        )
    }

    override suspend fun constructReferendaStatuses(
        selectedGovernanceOption: SupportedGovernanceOption,
        onChainReferenda: Collection<OnChainReferendum>,
        votingByReferenda: Map<ReferendumId, ReferendumVoting?>,
        thresholdByReferenda: Map<ReferendumId, ReferendumThreshold?>,
        tracksById: Map<TrackId, TrackInfo>,
        currentBlockNumber: BlockNumber,
    ): Map<ReferendumId, ReferendumStatus> {
        val chain = selectedGovernanceOption.assetWithChain.chain
        val governanceSource = governanceSourceRegistry.sourceFor(selectedGovernanceOption)

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
                        votingByReferenda = votingByReferenda,
                        thresholdByReferenda = thresholdByReferenda,
                        queuePositions = queuePositions,
                    )

                    is OnChainReferendumStatus.Approved -> {
                        val executionBlock = approvedReferendaExecutionBlocks[referendum.id]

                        if (executionBlock != null) {
                            val executeIn = blockDurationEstimator.timerUntil(executionBlock)
                            ReferendumStatus.Approved(executeIn = executeIn, since = status.since)
                        } else {
                            ReferendumStatus.Executed
                        }
                    }

                    is OnChainReferendumStatus.Rejected -> ReferendumStatus.NotExecuted.Rejected

                    is OnChainReferendumStatus.Cancelled -> ReferendumStatus.NotExecuted.Cancelled
                    is OnChainReferendumStatus.Killed -> ReferendumStatus.NotExecuted.Killed
                    is OnChainReferendumStatus.TimedOut -> ReferendumStatus.NotExecuted.TimedOut
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
                    add(State.CANCELLED, at = onChainReferendum.status.sinceOrThrow())
                }

                ReferendumStatus.NotExecuted.Killed -> {
                    add(State.CREATED, at = null)
                    add(State.KILLED, at = onChainReferendum.status.sinceOrThrow())
                }

                ReferendumStatus.NotExecuted.Rejected -> {
                    add(State.CREATED, at = null)
                    add(State.REJECTED, at = onChainReferendum.status.sinceOrThrow())
                }

                ReferendumStatus.NotExecuted.TimedOut -> {
                    add(State.CREATED, at = null)
                    add(State.TIMED_OUT, at = onChainReferendum.status.sinceOrThrow())
                }

                is ReferendumStatus.Approved -> {
                    add(State.CREATED, at = null)
                    // Approved status will be added in another place because this is a historical status but approved is an active status
                }

                ReferendumStatus.Executed -> {
                    add(State.CREATED, at = null)
                    add(State.APPROVED, at = onChainReferendum.status.sinceOrThrow())
                    add(State.EXECUTED, at = onChainReferendum.status.sinceOrThrow())
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
        votingByReferenda: Map<ReferendumId, ReferendumVoting?>,
        thresholdByReferenda: Map<ReferendumId, ReferendumThreshold?>,
        queuePositions: Map<ReferendumId, TrackQueue.Position>
    ): ReferendumStatus {
        return when {
            status.inQueue -> {
                val timeoutBlock = status.submitted + undecidingTimeout
                val timeOutIn = blockDurationEstimator.timerUntil(timeoutBlock)
                val positionInQueue = queuePositions.getValue(referendumId)

                ReferendumStatus.Ongoing.InQueue(timeOutIn, positionInQueue)
            }

            // confirming status from on-chain
            status.deciding?.confirming is ConfirmingSource.OnChain -> {
                confirmingStatusFromOnChain(status, blockDurationEstimator, track, referendumId, thresholdByReferenda)
            }

            // confirming status from threshold
            status.deciding?.confirming is ConfirmingSource.FromThreshold -> {
                val end = status.deciding!!.confirming.cast<ConfirmingSource.FromThreshold>().end
                val finishIn = blockDurationEstimator.timerUntil(end)

                val passing = thresholdByReferenda[referendumId]?.currentlyPassing() ?: false

                if (passing) {
                    ReferendumStatus.Ongoing.DecidingApprove(approveIn = finishIn)
                } else {
                    ReferendumStatus.Ongoing.DecidingReject(rejectIn = finishIn)
                }
            }

            // preparing
            else -> {
                val timeoutBlock = status.submitted + undecidingTimeout
                val timeOutIn = blockDurationEstimator.timerUntil(timeoutBlock)

                if (status.decisionDeposit != null) {
                    val preparedBlock = status.submitted + track.preparePeriod
                    val preparedIn = blockDurationEstimator.timerUntil(preparedBlock)

                    ReferendumStatus.Ongoing.Preparing(PreparingReason.DecidingIn(preparedIn), timeOutIn)
                } else {
                    ReferendumStatus.Ongoing.Preparing(PreparingReason.WaitingForDeposit, timeOutIn)
                }
            }
        }
    }

    private fun confirmingStatusFromOnChain(
        status: OnChainReferendumStatus.Ongoing,
        blockDurationEstimator: BlockDurationEstimator,
        track: TrackInfo,
        referendumId: ReferendumId,
        thresholdByReferenda: Map<ReferendumId, ReferendumThreshold?>,
    ): ReferendumStatus.Ongoing {
        val decidingStatus = status.deciding!!
        val referendumThreshold = thresholdByReferenda[referendumId]
        val delayedPassing = referendumThreshold?.projectedPassing()

        val isCurrentlyPassing = referendumThreshold?.currentlyPassing().orFalse()
        val isPassingAfterDelay = delayedPassing != null && delayedPassing.passingInFuture

        return when {
            // Confirmation period started block
            isCurrentlyPassing && decidingStatus.confirming.tillOrNull() != null -> {
                val approveBlock = decidingStatus.confirming.till()
                val approveIn = blockDurationEstimator.timerUntil(approveBlock)

                ReferendumStatus.Ongoing.Confirming(approveIn = approveIn)
            }

            // Deciding period that will be approved in delay + confirmation period block
            isPassingAfterDelay -> {
                val delay = delayedPassing!!.delayFraction
                val blocksToConfirmationPeriod = (delay * track.decisionPeriod.toBigDecimal()).toBigInteger()

                val approveBlock = decidingStatus.since + blocksToConfirmationPeriod + track.confirmPeriod
                val approveIn = blockDurationEstimator.timerUntil(approveBlock)

                ReferendumStatus.Ongoing.DecidingApprove(approveIn = approveIn)
            }

            // Reject block
            else -> {
                val rejectBlock = decidingStatus.since + track.decisionPeriod
                val rejectIn = blockDurationEstimator.timerUntil(rejectBlock)

                ReferendumStatus.Ongoing.DecidingReject(rejectIn)
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
