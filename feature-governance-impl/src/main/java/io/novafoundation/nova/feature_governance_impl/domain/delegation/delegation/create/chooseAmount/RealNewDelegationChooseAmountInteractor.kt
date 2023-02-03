package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseAmount

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.submitExtrinsicWithSelectedWalletAndWaitBlockInclusion
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import io.novafoundation.nova.feature_governance_api.data.repository.DelegationsRepository
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSource
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.create.chooseAmount.DelegateAssistant
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegation.create.chooseAmount.NewDelegationChooseAmountInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.blockDurationEstimator
import io.novafoundation.nova.runtime.state.selectedOption
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DELEGATION_ASSISTANT_CACHE_KEY = "RealNewDelegationChooseAmountInteractor.DelegationAssistant"

class RealNewDelegationChooseAmountInteractor(
    private val governanceSourceRegistry: GovernanceSourceRegistry,
    private val chainStateRepository: ChainStateRepository,
    private val selectedChainState: GovernanceSharedState,
    private val extrinsicService: ExtrinsicService,
    private val locksRepository: BalanceLocksRepository,
    private val computationalCache: ComputationalCache,
) : NewDelegationChooseAmountInteractor {

    override fun delegateAssistantFlow(coroutineScope: CoroutineScope): Flow<DelegateAssistant> {
        return computationalCache.useSharedFlow(DELEGATION_ASSISTANT_CACHE_KEY, coroutineScope) {
            val governanceOption = selectedChainState.selectedOption()

            voteAssistantFlowSuspend(governanceOption)
        }
    }

    override suspend fun estimateFee(
        amount: Balance,
        conviction: Conviction,
        delegate: AccountId,
        tracks: Collection<TrackId>
    ): Balance {
        val (chain, governanceSource) = useSelectedGovernance()

        return extrinsicService.estimateFee(chain) {
            delegate(governanceSource.delegationsRepository, amount, conviction, delegate, tracks)
        }
    }

    override suspend fun delegate(
        amount: Balance,
        conviction: Conviction,
        delegate: AccountId,
        tracks: Collection<TrackId>
    ): Result<ExtrinsicStatus.InBlock> {
        val (chain, governanceSource) = useSelectedGovernance()

        return extrinsicService.submitExtrinsicWithSelectedWalletAndWaitBlockInclusion(chain) {
            delegate(governanceSource.delegationsRepository, amount, conviction, delegate, tracks)
        }
    }

    private suspend fun ExtrinsicBuilder.delegate(
        delegationsRepository: DelegationsRepository,
        amount: Balance,
        conviction: Conviction,
        delegate: AccountId,
        tracks: Collection<TrackId>
    ) {
        with(delegationsRepository) {
            tracks.forEach { trackId ->
                delegate(delegate, trackId, amount, conviction)
            }
        }
    }

    private suspend fun voteAssistantFlowSuspend(
        selectedGovernanceOption: SupportedGovernanceOption,
    ): Flow<DelegateAssistant> {
        val chain = selectedGovernanceOption.assetWithChain.chain
        val chainAsset = selectedGovernanceOption.assetWithChain.asset

        val governanceSource = governanceSourceRegistry.sourceFor(selectedGovernanceOption)
        val voteLockingPeriod = governanceSource.convictionVoting.voteLockingPeriod(chain.id)

        val blockDurationEstimator = chainStateRepository.blockDurationEstimator(chain.id)

        val balanceLocksFlow = locksRepository.observeBalanceLocks(chain, chainAsset)

        return balanceLocksFlow.map { locks ->
            RealDelegateAssistant(
                balanceLocks = locks,
                blockDurationEstimator = blockDurationEstimator,
                voteLockingPeriod = voteLockingPeriod,
                votingLockId = governanceSource.convictionVoting.voteLockId
            )
        }
    }

    private suspend fun useSelectedGovernance(): Pair<Chain, GovernanceSource> {
        val option = selectedChainState.selectedOption()
        val source = governanceSourceRegistry.sourceFor(option)
        val chain = option.assetWithChain.chain

        return chain to source
    }
}
