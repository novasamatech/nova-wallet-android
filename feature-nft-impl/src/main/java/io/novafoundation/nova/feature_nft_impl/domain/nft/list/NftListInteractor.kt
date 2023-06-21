package io.novafoundation.nova.feature_nft_impl.domain.nft.list

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_api.data.repository.NftRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.utilityAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext

class NftListInteractor(
    private val accountRepository: AccountRepository,
    private val tokenRepository: TokenRepository,
    private val nftRepository: NftRepository,
) {

    fun userNftsFlow(): Flow<List<PricedNft>> {
        return accountRepository.selectedMetaAccountFlow()
            .flatMapLatest(nftRepository::allNftFlow)
            .map { nfts -> nfts.sortedBy { it.identifier } }
            .flatMapLatest { nfts ->
                val allUtilityAssets = nfts.map { it.chain.utilityAsset }.distinct()

                tokenRepository.observeTokens(allUtilityAssets).mapLatest { tokensByUtilityAsset ->
                    nfts.map { nft ->
                        PricedNft(
                            nft = nft,
                            nftPriceToken = tokensByUtilityAsset[nft.chain.utilityAsset.fullId]
                        )
                    }
                }
            }
    }

    suspend fun syncNftsList() = withContext(Dispatchers.Default) {
        nftRepository.initialNftSync(accountRepository.getSelectedMetaAccount(), forceOverwrite = true)
    }

    suspend fun fullSyncNft(nft: Nft) {
        nftRepository.fullNftSync(nft)
    }
}
