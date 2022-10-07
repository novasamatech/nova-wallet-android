package io.novafoundation.nova.feature_governance_impl.domain.referendum.list

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.common.utils.divideToDecimal
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendumStatus
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackInfo
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ayeFraction
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.nayFraction
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.proposalHash
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.supportThreshold
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.track
import io.novafoundation.nova.feature_governance_api.data.network.offchain.model.OffChainReferendumPreview
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSource
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.PreparingReason
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendaListInteractor
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumPreview
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumStatus
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumVoting
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.TotalIssuanceRepository
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.util.BlockDurationEstimator
import io.novafoundation.nova.runtime.util.timerUntil
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.math.BigInteger

class RealReferendaListInteractor(
    private val chainStateRepository: ChainStateRepository,
    private val governanceSourceRegistry: GovernanceSourceRegistry,
    private val selectedAssetState: SingleAssetSharedState,
    private val totalIssuanceRepository: TotalIssuanceRepository,
) : ReferendaListInteractor {

    override fun referendaFlow(voterAccountId: AccountId): Flow<List<ReferendumPreview>> {
        return flow {
            emitAll(referendaFlowSuspend(voterAccountId))
        }
    }

    private suspend fun referendaFlowSuspend(voterAccountId: AccountId): Flow<List<ReferendumPreview>> {
        val chain = selectedAssetState.chain()

        val governanceSource = governanceSourceRegistry.sourceFor(chain.id)

        val tracksById = governanceSource.referenda.getTracks(chain.id).associateBy(TrackInfo::id)

        return chainStateRepository.currentBlockNumberFlow(chain.id).map { currentBlockNumber ->
            val onChainReferenda = governanceSource.referenda.getOnChainReferenda(chain.id)
            val offChainInfo = governanceSource.offChainInfo.referendumPreviews(chain).associateBy(OffChainReferendumPreview::referendumId)

            val statuses = governanceSource.constructReferendaStatuses(
                chain = chain,
                onChainReferenda = onChainReferenda,
                tracksById = tracksById,
                currentBlockNumber = currentBlockNumber
            )

            val userVotes = governanceSource.convictionVoting.votingFor(voterAccountId, chain.id).flattenCastingVotes()

            onChainReferenda.map { onChainReferendum ->
                ReferendumPreview(
                    id = onChainReferendum.id,
                    offChainMetadata = offChainInfo[onChainReferendum.id]?.let {
                        ReferendumPreview.OffChainMetadata(it.title)
                    },
                    onChainMetadata = onChainReferendum.proposalHash()?.let {
                        ReferendumPreview.OnChainMetadata(it)
                    },
                    track = onChainReferendum.track()?.let { trackId ->
                        tracksById[trackId]?.let { trackInfo ->
                            ReferendumPreview.Track(name = trackInfo.name)
                        }
                    },
                    status = statuses.getValue(onChainReferendum.id),
                    voting = constructReferendumVoting(
                        referendum = onChainReferendum,
                        tracksById = tracksById,
                        currentBlockNumber = currentBlockNumber,
                        totalIssuance = totalIssuanceRepository.getTotalIssuance(chain.id)
                    ),
                    userVote = userVotes[onChainReferendum.id]
                )
            }
        }
    }

    private fun constructReferendumVoting(
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
                turnout = status.tally.support
            ),
            approval = ReferendumVoting.Approval(
                ayeFraction = status.tally.ayeFraction(),
                nayFraction = status.tally.nayFraction(),
                threshold = track.minApproval.threshold(elapsedSinceDecidingFraction)
            )
        )
    }

    private suspend fun GovernanceSource.constructReferendaStatuses(
        chain: Chain,
        onChainReferenda: Collection<OnChainReferendum>,
        tracksById: Map<TrackId, TrackInfo>,
        currentBlockNumber: BlockNumber,
    ): Map<ReferendumId, ReferendumStatus> {
        val blockTime = chainStateRepository.predictedBlockTime(chain.id)
        val blockDurationEstimator = BlockDurationEstimator(currentBlock = currentBlockNumber, blockTimeMillis = blockTime)

        val undecidingTimeout = referenda.undecidingTimeout(chain.id)

        val approvedReferendaIds = onChainReferenda.mapNotNull { referendum ->
            referendum.id.takeIf { referendum.status is OnChainReferendumStatus.Approved }
        }
        val approvedReferendaExecutionBlocks = referenda.getReferendaExecutionBlocks(chain.id, approvedReferendaIds)

        return onChainReferenda.associateBy(
            keySelector = { it.id },
            valueTransform = { referendum ->
                when (val status = referendum.status) {
                    is OnChainReferendumStatus.Ongoing -> constructOngoingStatus(
                        status = status,
                        blockDurationEstimator = blockDurationEstimator,
                        undecidingTimeout = undecidingTimeout,
                        track = tracksById.getValue(status.track)
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

    private fun constructOngoingStatus(
        status: OnChainReferendumStatus.Ongoing,
        blockDurationEstimator: BlockDurationEstimator,
        undecidingTimeout: BlockNumber,
        track: TrackInfo,
    ): ReferendumStatus {
        return when {
            status.inQueue -> {
                val timeoutBlock = status.submitted + undecidingTimeout
                val timeOutIn = blockDurationEstimator.timerUntil(timeoutBlock)

                ReferendumStatus.InQueue(timeOutIn)
            }

            // confirming
            status.deciding?.confirming != null -> {
                val approveBlock = status.deciding!!.confirming!!.till
                val approveIn = blockDurationEstimator.timerUntil(approveBlock)

                ReferendumStatus.Confirming(approveIn = approveIn)
            }

            // rejecting
            status.deciding != null -> {
                val rejectBlock = status.deciding!!.since + track.decisionPeriod
                val rejectIn = blockDurationEstimator.timerUntil(rejectBlock)

                ReferendumStatus.Deciding(rejectIn)
            }

            // preparing
            else -> {
                if (status.decisionDeposit != null) {
                    val preparedBlock = status.submitted + track.preparePeriod
                    val preparedIn = blockDurationEstimator.timerUntil(preparedBlock)

                    ReferendumStatus.Preparing(reason = PreparingReason.DecidingIn(preparedIn))
                } else {
                    ReferendumStatus.Preparing(reason = PreparingReason.WaitingForDeposit)
                }
            }
        }
    }

    private fun Map<TrackId, Voting>.flattenCastingVotes(): Map<ReferendumId, AccountVote> {
        return flatMap { (_, voting) ->
            when (voting) {
                is Voting.Casting -> voting.votes.toList()
                Voting.Delegating -> emptyList()
            }
        }.toMap()
    }
}
