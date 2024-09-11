package io.novafoundation.nova.feature_governance_impl.domain.referendum.vote

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AyeVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.flattenCastingVotes
import io.novafoundation.nova.feature_governance_api.data.repository.getTracksById
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.GovernanceVoteAssistant
import io.novafoundation.nova.feature_governance_api.domain.referendum.vote.VoteReferendumInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.blockDurationEstimator
import io.novafoundation.nova.runtime.state.selectedOption
import io.novasama.substrate_sdk_android.runtime.AccountId
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
        return computationalCache.useSharedFlow(VOTE_ASSISTANT_CACHE_KEY, scope) {
            val governanceOption = selectedChainState.selectedOption()
            val metaAccount = accountRepository.getSelectedMetaAccount()

            val voterAccountId = metaAccount.accountIdIn(governanceOption.assetWithChain.chain)!!

            voteAssistantFlowSuspend(governanceOption, voterAccountId, metaAccount.id, referendumId)
        }
    }

    override suspend fun estimateFee(amount: Balance, conviction: Conviction, referendumId: ReferendumId): Fee {
        val governanceOption = selectedChainState.selectedOption()
        val chain = governanceOption.assetWithChain.chain

        val vote = AyeVote(amount, conviction) // vote direction does not influence fee estimation
        val governanceSource = governanceSourceRegistry.sourceFor(governanceOption)

        return extrinsicService.estimateFee(chain, TransactionOrigin.SelectedWallet) {
            with(governanceSource.convictionVoting) {
                vote(referendumId, vote)
            }
        }
    }

    override suspend fun vote(
        vote: AccountVote,
        referendumId: ReferendumId,
    ): Result<ExtrinsicSubmission> {
        val governanceSelectedOption = selectedChainState.selectedOption()
        val governanceSource = governanceSourceRegistry.sourceFor(governanceSelectedOption)

        return extrinsicService.submitExtrinsic(governanceSelectedOption.assetWithChain.chain, TransactionOrigin.SelectedWallet) {
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
        referendumId: ReferendumId
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

        val selectedReferendumFlow = governanceSource.referenda.onChainReferendumFlow(chain.id, referendumId)
            .filterNotNull()

        val balanceLocksFlow = locksRepository.observeBalanceLocks(metaId, chain, chainAsset)

        return combine(votingInformation, selectedReferendumFlow, balanceLocksFlow) { (locksByTrack, voting, votedReferenda), selectedReferendum, locks ->
            val blockDurationEstimator = chainStateRepository.blockDurationEstimator(chain.id)

            RealGovernanceLocksEstimator(
                onChainReferendum = selectedReferendum,
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
}

