package io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock.list

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.data.repository.getTracksById
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.data.source.trackLocksFlowOrEmpty
import io.novafoundation.nova.feature_governance_api.domain.locks.ClaimSchedule
import io.novafoundation.nova.feature_governance_api.domain.locks.ClaimSchedule.UnlockChunk
import io.novafoundation.nova.feature_governance_api.domain.locks.RealClaimScheduleCalculator
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.util.BlockDurationEstimator
import io.novafoundation.nova.runtime.util.timerUntil
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

interface GovernanceLocksOverviewInteractor {

    fun locksOverviewFlow(scope: CoroutineScope): Flow<GovernanceLocksOverview>
}

private class IntermediateData(
    val voting: Map<TrackId, Voting>,
    val currentBlockNumber: BlockNumber,
    val onChainReferenda: Map<ReferendumId, OnChainReferendum>,
    val durationEstimator: BlockDurationEstimator,
)

private const val LOCKS_OVERVIEW_KEY = "RealGovernanceLocksOverviewInteractor.LOCKS_OVERVIEW_KEY"

class RealGovernanceLocksOverviewInteractor(
    private val selectedAssetState: SingleAssetSharedState,
    private val governanceSourceRegistry: GovernanceSourceRegistry,
    private val chainStateRepository: ChainStateRepository,
    private val computationalCache: ComputationalCache,
    private val accountRepository: AccountRepository,
) : GovernanceLocksOverviewInteractor {

    override fun locksOverviewFlow(scope: CoroutineScope): Flow<GovernanceLocksOverview> {
        return computationalCache.useSharedFlow(LOCKS_OVERVIEW_KEY, scope) {
            val chain = selectedAssetState.chain()
            val metaAccount = accountRepository.getSelectedMetaAccount()
            val voterAccountId = metaAccount.accountIdIn(chain)

            locksOverviewFlowSuspend(voterAccountId, chain)
        }
    }

    private suspend fun locksOverviewFlowSuspend(voterAccountId: AccountId?, chain: Chain): Flow<GovernanceLocksOverview> {
        val governanceSource = governanceSourceRegistry.sourceFor(chain.id)
        val tracksById = governanceSource.referenda.getTracksById(chain.id)
        val undecidingTimeout = governanceSource.referenda.undecidingTimeout(chain.id)
        val voteLockingPeriod = governanceSource.convictionVoting.voteLockingPeriod(chain.id)

        val trackLocksFlow = governanceSource.convictionVoting.trackLocksFlowOrEmpty(voterAccountId, chain.id)

        val intermediateFlow = chainStateRepository.currentBlockNumberFlow(chain.id).map { currentBlockNumber ->
            val onChainReferenda = governanceSource.referenda.getAllOnChainReferenda(chain.id).associateBy(OnChainReferendum::id)
            val voting = voterAccountId?.let { governanceSource.convictionVoting.votingFor(voterAccountId, chain.id) }.orEmpty()
            val blockTime = chainStateRepository.predictedBlockTime(chain.id)
            val durationEstimator = BlockDurationEstimator(currentBlockNumber, blockTime)

            IntermediateData(voting, currentBlockNumber, onChainReferenda, durationEstimator)
        }

        return combine(intermediateFlow, trackLocksFlow) { intermediateData, trackLocks ->
            val claimScheduleCalculator = with(intermediateData) {
                RealClaimScheduleCalculator(voting, currentBlockNumber, onChainReferenda, tracksById, undecidingTimeout, voteLockingPeriod, trackLocks)
            }

            val claimSchedule = claimScheduleCalculator.estimateClaimSchedule()
                .toOverviewLocks(intermediateData.durationEstimator)

            GovernanceLocksOverview(
                totalLocked = claimScheduleCalculator.totalGovernanceLock(),
                claimSchedule = claimSchedule
            )
        }
    }

    private fun ClaimSchedule.toOverviewLocks(durationEstimator: BlockDurationEstimator): List<GovernanceLocksOverview.Lock> {
        return chunks.map { chunk ->
            when(chunk) {
                is UnlockChunk.Claimable -> GovernanceLocksOverview.Lock.Claimable(chunk.amount, chunk.actions)

                is UnlockChunk.Pending -> GovernanceLocksOverview.Lock.Pending(
                    amount = chunk.amount,
                    timer = durationEstimator.timerUntil(chunk.claimableAt)
                )
            }
        }
    }
}
