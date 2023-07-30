package io.novafoundation.nova.feature_nft_impl.domain.nft.search

import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_api.data.model.NftDetails
import io.novafoundation.nova.feature_nft_api.data.repository.NftRepository
import io.novafoundation.nova.feature_nft_impl.domain.common.mapNftDetailsToListItem
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

@ScreenScope
class NftSearchInteractor @Inject constructor(
    private val accountRepository: AccountRepository,
    private val nftRepository: NftRepository
) {

    fun sendNftSearch(queryFlow: Flow<String>): Flow<Map<Chain, List<SendNftListItem>>> {
        return combine(getUserNfts(), queryFlow) { nfts, query ->
            mapNftToNftDetails(nfts)
                .map(::mapNftDetailsToListItem)
                .filterByQuery(query)
                .groupBy { nftDetails -> nftDetails.chain }
        }
    }

    private fun getUserNfts(): Flow<List<Nft>> {
        return accountRepository.selectedMetaAccountFlow()
            .flatMapLatest(nftRepository::allNftFlow)
    }

    private suspend fun mapNftToNftDetails(nfts: List<Nft>): List<NftDetails> {
        return nfts
            .onEach { nft ->
                val nftLocal = nftRepository.getLocalNft(nft.identifier)
                if (!nftLocal.wholeDetailsLoaded) {
                    nftRepository.fullNftSync(nft)
                }
            }
            .mapNotNull { nftRepository.nftDetails(it.identifier).firstOrNull() }
    }

    private fun List<SendNftListItem>.filterByQuery(query: String): List<SendNftListItem> {
        return filter { query in it.name || query in it.collectionName }
    }
}
