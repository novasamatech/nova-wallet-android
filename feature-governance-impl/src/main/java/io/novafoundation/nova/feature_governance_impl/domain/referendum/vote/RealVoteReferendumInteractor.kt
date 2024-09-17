package io.novafoundation.nova.feature_governance_impl.domain.referendum.vote

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.multiResult.RetriableMultiResult
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.OnChainReferendum
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.flattenCastingVotes
import io.novafoundation.nova.feature_governance_api.data.repository.getTracksById
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSource
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.GovernanceVoteAssistant
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.VoteReferendumInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.blockDurationEstimator
import io.novafoundation.nova.runtime.state.selectedOption
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.BatchMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

private const val VOTE_ASSISTANT_CACHE_KEY = "RealVoteReferendumInteractor.VoteAssistant"

class RealVoteReferendumInteractor(
    private val governanceSourceRegistry: GovernanceSourceRegistry,
    private val chainStateRepository: ChainStateRepository,
    private val selectedChainState: GovernanceSharedState,
    private val accountRepository: AccountRepository,
    private val extrinsicService: ExtrinsicService,
    private val locksRepository: BalanceLocksRepository,
    private val computationalCache: ComputationalCache,
) : VoteReferendumInteractor {

    override fun voteAssistantFlow(referendumId: ReferendumId, scope: CoroutineScope): Flow<GovernanceVoteAssistant> {
        return voteAssistantFlow(listOf(referendumId), scope)
    }

    override fun voteAssistantFlow(referendaIds: List<ReferendumId>, scope: CoroutineScope): Flow<GovernanceVoteAssistant> {
        return computationalCache.useSharedFlow(VOTE_ASSISTANT_CACHE_KEY, scope) {
            val governanceOption = selectedChainState.selectedOption()
            val metaAccount = accountRepository.getSelectedMetaAccount()

            val voterAccountId = metaAccount.accountIdIn(governanceOption.assetWithChain.chain)!!

            voteAssistantFlowSuspend(governanceOption, voterAccountId, metaAccount.id, referendaIds)
        }
    }

    override suspend fun estimateFee(votes: Map<ReferendumId, AccountVote>): Fee {
        val governanceOption = selectedChainState.selectedOption()
        val chain = governanceOption.assetWithChain.chain

        val governanceSource = governanceSourceRegistry.sourceFor(governanceOption)

        return extrinsicService.estimateMultiFee(chain, TransactionOrigin.SelectedWallet) {
            with(governanceSource.convictionVoting) {
                votes.forEach { (referendumId, vote) -> vote(referendumId, vote) }
            }
        }
    }

    override suspend fun estimateFee(referendumId: ReferendumId, vote: AccountVote): Fee {
        val governanceOption = selectedChainState.selectedOption()
        val chain = governanceOption.assetWithChain.chain

        val governanceSource = governanceSourceRegistry.sourceFor(governanceOption)

        return extrinsicService.estimateFee(chain, TransactionOrigin.SelectedWallet) {
            with(governanceSource.convictionVoting) {
                vote(referendumId, vote)
            }
        }
    }

    override suspend fun voteReferenda(votes: Map<ReferendumId, AccountVote>): RetriableMultiResult<ExtrinsicStatus.InBlock> {
        val governanceOption = selectedChainState.selectedOption()
        val chain = governanceOption.assetWithChain.chain

        val governanceSource = governanceSourceRegistry.sourceFor(governanceOption)

        return extrinsicService.submitMultiExtrinsicAwaitingInclusion(
            chain = chain,
            origin = TransactionOrigin.SelectedWallet,
            submissionOptions = ExtrinsicService.SubmissionOptions(batchMode = BatchMode.BATCH_ALL)
        ) {
            with(governanceSource.convictionVoting) {
                votes.forEach { (referendumId, vote) -> vote(referendumId, vote) }
            }
        }
    }

    override suspend fun voteReferendum(referendumId: ReferendumId, vote: AccountVote): Result<ExtrinsicSubmission> {
        val governanceOption = selectedChainState.selectedOption()
        val chain = governanceOption.assetWithChain.chain

        val governanceSource = governanceSourceRegistry.sourceFor(governanceOption)

        return extrinsicService.submitExtrinsic(chain, TransactionOrigin.SelectedWallet) {
            with(governanceSource.convictionVoting) {
                vote(referendumId, vote)
            }
        }
    }

    override suspend fun isAbstainSupported(): Boolean {
        val governanceSelectedOption = selectedChainState.selectedOption()
        val governanceSource = governanceSourceRegistry.sourceFor(governanceSelectedOption)

        return governanceSource.convictionVoting.isAbstainVotingAvailable()
    }

    private suspend fun voteAssistantFlowSuspend(
        selectedGovernanceOption: SupportedGovernanceOption,
        voterAccountId: AccountId,
        metaId: Long,
        referendaIds: List<ReferendumId>
    ): Flow<GovernanceVoteAssistant> {
        val chain = selectedGovernanceOption.assetWithChain.chain
        val chainAsset = selectedGovernanceOption.assetWithChain.asset

        val governanceSource = governanceSourceRegistry.sourceFor(selectedGovernanceOption)
        val tracks = governanceSource.referenda.getTracksById(chain.id)
        val undecidingTimeout = governanceSource.referenda.undecidingTimeout(chain.id)
        val voteLockingPeriod = governanceSource.convictionVoting.voteLockingPeriod(chain.id)

        val votingInformation = governanceSource.convictionVoting.trackLocksFlow(voterAccountId, chainAsset.fullId).map { locksByTrack ->
            val voting = governanceSource.convictionVoting.votingFor(voterAccountId, chain.id)
            val accountVotesByReferendumId = voting.flattenCastingVotes()
            val votedReferenda = governanceSource.referenda.getOnChainReferenda(chain.id, accountVotesByReferendumId.keys)

            Triple(locksByTrack, voting, votedReferenda)
        }

        val selectedReferendaFlow = getOnChainReferendaFlow(governanceSource, chain, referendaIds)

        val balanceLocksFlow = locksRepository.observeBalanceLocks(metaId, chain, chainAsset)

        return combine(votingInformation, selectedReferendaFlow, balanceLocksFlow) { (locksByTrack, voting, votedReferenda), selectedReferenda, locks ->
            val blockDurationEstimator = chainStateRepository.blockDurationEstimator(chain.id)

            RealGovernanceLocksEstimator(
                onChainReferenda = selectedReferenda,
                balanceLocks = locks,
                governanceLocksByTrack = locksByTrack,
                voting = voting,
                votedReferenda = votedReferenda,
                blockDurationEstimator = blockDurationEstimator,
                tracks = tracks,
                undecidingTimeout = undecidingTimeout,
                voteLockingPeriod = voteLockingPeriod,
                votingLockId = governanceSource.convictionVoting.voteLockId
            )
        }
    }

    private suspend fun getOnChainReferendaFlow(
        governanceSource: GovernanceSource,
        chain: Chain,
        referendaIds: List<ReferendumId>
    ): Flow<List<OnChainReferendum>> {
        return if (referendaIds.size == 1) {
            governanceSource.referenda.onChainReferendumFlow(chain.id, referendaIds.first())
                .filterNotNull()
                .map { listOf(it) }
        } else {
            flowOf { governanceSource.referenda.getOnChainReferenda(chain.id, referendaIds) }
                .map { it.values.toList() }
        }
    }
}
