package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.revoke

import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.multiResult.RetriableMultiResult
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.watch.ExtrinsicWatchResult
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.requireIdOfSelectedMetaAccountIn
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.delegations
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.lockDuration
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSource
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.delegators.model.DelegatorVote
import io.novafoundation.nova.feature_governance_api.domain.track.matchWith
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.track.TracksUseCase
import io.novafoundation.nova.feature_governance_impl.domain.track.tracksByIdOf
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.extrinsic.multi.CallBuilder
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.blockDurationEstimator
import io.novafoundation.nova.runtime.state.selectedOption
import kotlinx.coroutines.flow.Flow

interface RevokeDelegationsInteractor {

    suspend fun calculateFee(trackIds: Collection<TrackId>): Fee

    suspend fun revokeDelegations(trackIds: Collection<TrackId>): RetriableMultiResult<ExtrinsicWatchResult<ExtrinsicStatus.InBlock>>

    fun revokeDelegationDataFlow(trackIds: Collection<TrackId>): Flow<RevokeDelegationData>
}

class RealRevokeDelegationsInteractor(
    private val extrinsicService: ExtrinsicService,
    private val governanceSharedState: GovernanceSharedState,
    private val governanceSourceRegistry: GovernanceSourceRegistry,
    private val chainStateRepository: ChainStateRepository,
    private val accountRepository: AccountRepository,
    private val tracksUseCase: TracksUseCase,
) : RevokeDelegationsInteractor {

    override suspend fun calculateFee(trackIds: Collection<TrackId>): Fee {
        val (chain, source) = useSelectedGovernance()

        return extrinsicService.estimateMultiFee(chain, TransactionOrigin.SelectedWallet) {
            revokeDelegations(source, trackIds)
        }
    }

    override suspend fun revokeDelegations(trackIds: Collection<TrackId>): RetriableMultiResult<ExtrinsicWatchResult<ExtrinsicStatus.InBlock>> {
        val (chain, source) = useSelectedGovernance()

        return extrinsicService.submitMultiExtrinsicAwaitingInclusion(chain, TransactionOrigin.SelectedWallet) {
            revokeDelegations(source, trackIds)
        }
    }

    override fun revokeDelegationDataFlow(trackIds: Collection<TrackId>): Flow<RevokeDelegationData> {
        return flowOf {
            val (chain, source, chainAsset) = useSelectedGovernance()
            val accountId = accountRepository.requireIdOfSelectedMetaAccountIn(chain)

            val blockDurationEstimator = chainStateRepository.blockDurationEstimator(chain.id)
            val tracks = tracksUseCase.tracksByIdOf(trackIds)

            val delegations = source.convictionVoting.votingFor(accountId, chain.id, trackIds)
                .delegations()
                .matchWith(tracks)

            val undelegatingPeriod = source.convictionVoting.voteLockingPeriod(chain.id)

            val maxUndelegateDurationInBlocks = delegations
                .maxOfOrNull { (_, delegation) -> delegation.conviction.lockDuration(undelegatingPeriod) }
                .orZero()

            val maxUndelegateDuration = blockDurationEstimator.durationOf(maxUndelegateDurationInBlocks)

            val delegationsOverview = DelegatorVote(delegations.values, chainAsset)

            RevokeDelegationData(maxUndelegateDuration, delegationsOverview, delegations)
        }
    }

    private suspend fun CallBuilder.revokeDelegations(
        source: GovernanceSource,
        trackIds: Collection<TrackId>
    ) {
        trackIds.forEach { trackId ->
            with(source.delegationsRepository) { undelegate(trackId) }
        }
    }

    private suspend fun useSelectedGovernance(): Triple<Chain, GovernanceSource, Chain.Asset> {
        val option = governanceSharedState.selectedOption()
        val source = governanceSourceRegistry.sourceFor(option)
        val (chain, chainAsset) = option.assetWithChain

        return Triple(chain, source, chainAsset)
    }
}
