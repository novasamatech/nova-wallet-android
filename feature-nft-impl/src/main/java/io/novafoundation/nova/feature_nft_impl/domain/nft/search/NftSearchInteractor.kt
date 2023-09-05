package io.novafoundation.nova.feature_nft_impl.domain.nft.search

import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.core_db.model.NftLocal
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
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@ScreenScope
class NftSearchInteractor @Inject constructor(
    private val accountRepository: AccountRepository,
    private val nftRepository: NftRepository
) {

    fun sendNftSearch(queryFlow: Flow<String>): Flow<Map<Chain, List<SendNftListItem>>> {
        return combine(getUserNfts(), queryFlow) { nfts, query ->
            nfts.filterByQuery(query)
                .groupBy { nftDetails -> nftDetails.chain }
        }
    }

    private fun getUserNfts(): Flow<List<SendNftListItem>> {
        return accountRepository.selectedMetaAccountFlow()
            .flatMapLatest(nftRepository::allNftFlow)
            .map { nfts ->
                nfts.filter { nftRepository.isNftTypeSupportedForSend(it.type) }
            }
            .map { nfts ->
                val nftLocals = nftRepository.getLocalNfts(nfts.map { it.identifier })
                fullNftSyncIfNotSynced(nftLocals, nfts)
                nftLocals
                    .mapNotNull { nftRepository.nftDetails(it.identifier).firstOrNull() }
                    .map(::mapNftDetailsToListItem)
            }
    }

    private suspend fun fullNftSyncIfNotSynced(
        nftLocals: List<NftLocal>,
        nfts: List<Nft>
    ) {
        nftLocals.onEach { nftLocal ->
            val nft = nfts.find { it.identifier == nftLocal.identifier }
            if (!nftLocal.wholeDetailsLoaded && nft != null) {
                nftRepository.fullNftSync(nft)
            }
        }
    }

    private fun List<SendNftListItem>.filterByQuery(query: String): List<SendNftListItem> {
        return filter { query in it.name || query in it.collectionName }
    }
}
