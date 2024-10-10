package io.novafoundation.nova.feature_governance_impl.domain.referendum.tindergov

import io.novafoundation.nova.common.domain.filterLoaded
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_governance_api.data.model.TinderGovBasketItem
import io.novafoundation.nova.feature_governance_api.data.model.VotingPower
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumCall
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendaState
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumPreview
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.Voter
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.toCallOrNull
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.user
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.data.repository.tindergov.TinderGovVotingPowerRepository
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call.ReferendumPreImageParser
import io.novafoundation.nova.feature_governance_impl.domain.referendum.list.ReferendaSharedComputation
import io.novafoundation.nova.feature_governance_impl.domain.referendum.list.filtering.ReferendaFilteringProvider
import io.novafoundation.nova.feature_governance_impl.domain.referendum.tindergov.validation.StartStakingLandingValidationSystem
import io.novafoundation.nova.feature_governance_impl.domain.referendum.tindergov.validation.StartSwipeGovValidationFailure
import io.novafoundation.nova.feature_governance_impl.domain.referendum.tindergov.validation.StartSwipeGovValidationPayload
import io.novafoundation.nova.feature_governance_impl.domain.referendum.tindergov.validation.startSwipeGovValidation
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.getCurrentAsset
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.availableToVote
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.selectedOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

sealed interface VotingPowerState {

    object Empty : VotingPowerState

    class InsufficientAmount(val votingPower: VotingPower) : VotingPowerState

    class SufficientAmount(val votingPower: VotingPower) : VotingPowerState
}

interface TinderGovInteractor {

    fun observeReferendaState(coroutineScope: CoroutineScope): Flow<ReferendaState>

    fun observeReferendaAvailableToVote(coroutineScope: CoroutineScope): Flow<List<ReferendumPreview>>

    suspend fun getReferendumAmount(referendumPreview: ReferendumPreview): ReferendumCall.TreasuryRequest?

    suspend fun setVotingPower(votingPower: VotingPower)

    suspend fun getVotingPower(metaId: Long, chainId: ChainId): VotingPower?

    suspend fun getVotingPowerState(): VotingPowerState

    suspend fun awaitAllItemsVoted(coroutineScope: CoroutineScope, basket: List<TinderGovBasketItem>)

    fun startSwipeGovValidationSystem(): StartStakingLandingValidationSystem
}

class RealTinderGovInteractor(
    private val governanceSharedState: GovernanceSharedState,
    private val referendaSharedComputation: ReferendaSharedComputation,
    private val accountRepository: AccountRepository,
    private val preImageParser: ReferendumPreImageParser,
    private val tinderGovVotingPowerRepository: TinderGovVotingPowerRepository,
    private val referendaFilteringProvider: ReferendaFilteringProvider,
    private val assetUseCase: AssetUseCase
) : TinderGovInteractor {

    override fun observeReferendaState(coroutineScope: CoroutineScope): Flow<ReferendaState> = flowOfAll {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val chain = governanceSharedState.chain()
        val accountId = metaAccount.accountIdIn(chain)
        val voter = accountId?.let { Voter.user(accountId) }

        referendaSharedComputation.referenda(
            metaAccount,
            voter,
            governanceSharedState.selectedOption(),
            coroutineScope
        ).filterLoaded()
    }

    override fun observeReferendaAvailableToVote(coroutineScope: CoroutineScope): Flow<List<ReferendumPreview>> {
        return observeReferendaState(coroutineScope)
            .map { filterAvailableToVoteReferenda(it) }
    }

    override suspend fun getReferendumAmount(referendumPreview: ReferendumPreview): ReferendumCall.TreasuryRequest? {
        val selectedGovernanceOption = governanceSharedState.selectedOption()
        val chain = selectedGovernanceOption.assetWithChain.chain

        val referendumProposal = referendumPreview.onChainMetadata?.proposal
        val referendumProposalCall = referendumProposal?.toCallOrNull()
        val referendumCall = referendumProposalCall?.let { preImageParser.parsePreimageCall(it.call, chain) }

        return referendumCall as? ReferendumCall.TreasuryRequest
    }

    override suspend fun setVotingPower(votingPower: VotingPower) {
        tinderGovVotingPowerRepository.setVotingPower(votingPower)
    }

    override suspend fun getVotingPower(metaId: Long, chainId: ChainId): VotingPower? {
        return tinderGovVotingPowerRepository.getVotingPower(metaId, chainId)
    }

    override suspend fun getVotingPowerState(): VotingPowerState {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val chain = governanceSharedState.chain()
        val votingPower = getVotingPower(metaAccount.id, chain.id) ?: return VotingPowerState.Empty
        val asset = assetUseCase.getCurrentAsset()

        return if (asset.availableToVote() >= votingPower.amount) {
            VotingPowerState.SufficientAmount(votingPower)
        } else {
            VotingPowerState.InsufficientAmount(votingPower)
        }
    }

    override suspend fun awaitAllItemsVoted(coroutineScope: CoroutineScope, basket: List<TinderGovBasketItem>) {
        observeReferendaState(coroutineScope)
            .filter { referendaState ->
                val referenda = referendaState.referenda.associateBy { it.id }
                val allBasketItemsVoted = basket.all {
                    val referendum = referenda[it.referendumId]
                    referendum?.referendumVote != null
                }

                allBasketItemsVoted
            }.first()
    }

    override fun startSwipeGovValidationSystem(): StartStakingLandingValidationSystem {
        return ValidationSystem.startSwipeGovValidation()
    }

    private fun TinderGovBasketItem.isItemNotAvailableToVote(
        availableToVoteReferenda: Set<ReferendumId>,
        asset: Asset
    ): Boolean {
        val notEnoughBalance = this.amount > asset.availableToVote()
        return (this.referendumId !in availableToVoteReferenda) || notEnoughBalance
    }

    private suspend fun filterAvailableToVoteReferenda(referendaState: ReferendaState): List<ReferendumPreview> {
        return referendaFilteringProvider.filterAvailableToVoteReferenda(referendaState.referenda, referendaState.voting)
    }
}
