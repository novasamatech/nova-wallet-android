package io.novafoundation.nova.feature_governance_impl.domain.referendum.tindergov

import io.novafoundation.nova.common.domain.filterLoaded
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_governance_api.data.model.TinderGovBasketItem
import io.novafoundation.nova.feature_governance_api.data.model.VotingPower
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumCall
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumPreview
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.Voter
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.toCallOrNull
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.user
import io.novafoundation.nova.feature_governance_api.domain.tindergov.TinderGovInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.summary.v2.ReferendumSummaryDataSource
import io.novafoundation.nova.feature_governance_impl.data.repository.tindergov.TinderGovBasketRepository
import io.novafoundation.nova.feature_governance_impl.data.repository.tindergov.TinderGovVotingPowerRepository
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call.ReferendumPreImageParser
import io.novafoundation.nova.feature_governance_impl.domain.referendum.list.ReferendaSharedComputation
import io.novafoundation.nova.feature_governance_impl.domain.referendum.list.repository.ReferendaCommonRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.runtime.ext.summaryApiOrNull
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.selectedOption
import java.math.BigInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RealTinderGovInteractor(
    private val governanceSharedState: GovernanceSharedState,
    private val referendaCommonRepository: ReferendaCommonRepository,
    private val referendaSharedComputation: ReferendaSharedComputation,
    private val accountRepository: AccountRepository,
    private val referendumSummaryDataSource: ReferendumSummaryDataSource,
    private val preImageParser: ReferendumPreImageParser,
    private val tinderGovBasketRepository: TinderGovBasketRepository,
    private val walletRepository: WalletRepository,
    private val tinderGovVotingPowerRepository: TinderGovVotingPowerRepository,
) : TinderGovInteractor {

    override fun observeReferendaAvailableToVote(coroutineScope: CoroutineScope): Flow<List<ReferendumPreview>> {
        return flowOfAll {
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
                .map { referendaCommonRepository.filterAvailableToVoteReferenda(it.referenda, it.voting) }
        }
    }

    override fun observeTinderGovBasket(): Flow<List<TinderGovBasketItem>> {
        return flowOfAll {
            val metaAccount = accountRepository.getSelectedMetaAccount()
            val chain = governanceSharedState.chain()

            tinderGovBasketRepository.observeBasket(metaAccount.id, chain.id)
        }
    }

    override suspend fun addItemToBasket(referendumId: ReferendumId, voteType: VoteType) {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val chain = governanceSharedState.chain()

        tinderGovBasketRepository.add(
            TinderGovBasketItem(
                metaId = metaAccount.id,
                chainId = chain.id,
                referendumId = referendumId,
                vote = voteType,
                conviction = Conviction.None,
                amount = BigInteger.ZERO
            )
        )
    }

    override suspend fun loadReferendumSummary(id: ReferendumId): String? {
        val chain = governanceSharedState.chain()
        val summaryApi = chain.summaryApiOrNull() ?: return null
        return referendumSummaryDataSource.loadSummary(chain, id, summaryApi.url)
    }

    override suspend fun loadReferendumAmount(referendumPreview: ReferendumPreview): BigInteger? {
        val selectedGovernanceOption = governanceSharedState.selectedOption()
        val chain = selectedGovernanceOption.assetWithChain.chain

        val referendumProposal = referendumPreview.onChainMetadata?.proposal
        val referendumProposalCall = referendumProposal?.toCallOrNull()
        val referendumCall = referendumProposalCall?.let { preImageParser.parsePreimageCall(it.call, chain.id) }

        return if (referendumCall is ReferendumCall.TreasuryRequest) {
            referendumCall.amount
        } else {
            null
        }
    }

    override suspend fun setVotingPower(chainId: ChainId, amount: BigInteger, conviction: Conviction) {
        tinderGovVotingPowerRepository.setVotingPower(VotingPower(chainId, amount, conviction))
    }

    override suspend fun getVotingPower(chainId: ChainId): VotingPower? {
        return tinderGovVotingPowerRepository.getVotingPower(chainId)
    }

    override suspend fun isSufficientAmountToVote(): Boolean {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val chain = governanceSharedState.chain()
        val votingPower = getVotingPower(chain.id) ?: return false
        val asset = walletRepository.getAsset(metaAccount.id, chain.utilityAsset) ?: return false

        return asset.totalInPlanks >= votingPower.amount
    }
}
