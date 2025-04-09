package io.novafoundation.nova.feature_governance_impl.domain.referendum.tindergov

import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_governance_api.data.model.TinderGovBasketItem
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSource
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.data.repository.tindergov.TinderGovBasketRepository
import io.novafoundation.nova.feature_governance_impl.data.repository.tindergov.TinderGovVotingPowerRepository
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.getCurrentAsset
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.selectedOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

interface TinderGovBasketInteractor {

    fun observeTinderGovBasket(): Flow<List<TinderGovBasketItem>>

    suspend fun getTinderGovBasket(): List<TinderGovBasketItem>

    suspend fun addItemToBasket(referendumId: ReferendumId, voteType: VoteType)

    suspend fun removeReferendumFromBasket(item: TinderGovBasketItem)

    suspend fun removeBasketItems(items: Collection<TinderGovBasketItem>)

    suspend fun isBasketEmpty(): Boolean

    suspend fun clearBasket()

    suspend fun getBasketItemsToRemove(coroutineScope: CoroutineScope): List<TinderGovBasketItem>

    suspend fun awaitAllItemsVoted(coroutineScope: CoroutineScope, basket: List<TinderGovBasketItem>)
}

class RealTinderGovBasketInteractor(
    private val governanceSharedState: GovernanceSharedState,
    private val accountRepository: AccountRepository,
    private val tinderGovBasketRepository: TinderGovBasketRepository,
    private val tinderGovVotingPowerRepository: TinderGovVotingPowerRepository,
    private val assetUseCase: AssetUseCase,
    private val tinderGovInteractor: TinderGovInteractor,
    private val governanceSourceRegistry: GovernanceSourceRegistry,
) : TinderGovBasketInteractor {

    override fun observeTinderGovBasket(): Flow<List<TinderGovBasketItem>> {
        return flowOfAll {
            val metaAccount = accountRepository.getSelectedMetaAccount()
            val chain = governanceSharedState.chain()

            tinderGovBasketRepository.observeBasket(metaAccount.id, chain.id)
        }
    }

    override suspend fun getTinderGovBasket(): List<TinderGovBasketItem> {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val chain = governanceSharedState.chain()

        return tinderGovBasketRepository.getBasket(metaAccount.id, chain.id)
    }

    override suspend fun addItemToBasket(referendumId: ReferendumId, voteType: VoteType) {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val chain = governanceSharedState.chain()

        val votingPower = tinderGovVotingPowerRepository.getVotingPower(metaAccount.id, chain.id)!!

        tinderGovBasketRepository.add(
            TinderGovBasketItem(
                metaId = metaAccount.id,
                chainId = chain.id,
                referendumId = referendumId,
                voteType = voteType,
                conviction = votingPower.conviction,
                amount = votingPower.amount
            )
        )
    }

    override suspend fun removeReferendumFromBasket(item: TinderGovBasketItem) {
        tinderGovBasketRepository.remove(item)
    }

    override suspend fun removeBasketItems(items: Collection<TinderGovBasketItem>) {
        tinderGovBasketRepository.remove(items)
    }

    override suspend fun isBasketEmpty(): Boolean {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val chain = governanceSharedState.chain()

        return tinderGovBasketRepository.isBasketEmpty(metaAccount.id, chain.id)
    }

    override suspend fun clearBasket() {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val chain = governanceSharedState.chain()

        tinderGovBasketRepository.clearBasket(metaAccount.id, chain.id)
    }

    override suspend fun getBasketItemsToRemove(coroutineScope: CoroutineScope): List<TinderGovBasketItem> {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val chain = governanceSharedState.chain()
        val asset = assetUseCase.getCurrentAsset()
        val basket = tinderGovBasketRepository.getBasket(metaAccount.id, chain.id)

        val availableToVoteReferenda = tinderGovInteractor.observeReferendaAvailableToVote(coroutineScope).first()
            .mapToSet { it.id }

        val governanceSource = governanceSourceRegistry.sourceFor(governanceSharedState.selectedOption())
        return basket.filter { it.isItemNotAvailableToVote(availableToVoteReferenda, asset, governanceSource) }
    }

    override suspend fun awaitAllItemsVoted(coroutineScope: CoroutineScope, basket: List<TinderGovBasketItem>) {
        tinderGovInteractor.observeReferendaState(coroutineScope)
            .filter { referendaState ->
                val referenda = referendaState.referenda.associateBy { it.id }
                val allBasketItemsVoted = basket.all {
                    val referendum = referenda[it.referendumId]
                    referendum?.referendumVote != null
                }

                allBasketItemsVoted
            }.first()
    }

    private suspend fun TinderGovBasketItem.isItemNotAvailableToVote(
        availableToVoteReferenda: Set<ReferendumId>,
        asset: Asset,
        governanceSource: GovernanceSource,
    ): Boolean {
        val notEnoughBalance = this.amount > governanceSource.convictionVoting.maxAvailableForVote(asset)
        return (this.referendumId !in availableToVoteReferenda) || notEnoughBalance
    }
}
