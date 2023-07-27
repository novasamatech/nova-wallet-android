package io.novafoundation.nova.feature_nft_impl.domain.nft.list

import io.novafoundation.nova.core_db.model.NftLocal
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_api.data.repository.NftRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

class NftListInteractor(
    private val accountRepository: AccountRepository,
    private val tokenRepository: TokenRepository,
    private val nftRepository: NftRepository,
    private val chainStateRepository: ChainStateRepository
) {

    fun userNftsFlow(): Flow<List<PricedNft>> {
        return accountRepository.selectedMetaAccountFlow()
            .flatMapLatest(nftRepository::allNftFlow)
            .map { nfts -> nfts.sortedBy { it.identifier } }
            .onEach {
                nftRepository.removeOldPendingTransactions(
                    myNftIds = it.map { it.identifier }
                )
            }
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

    @OptIn(ExperimentalCoroutinesApi::class)
    fun subscribeNftOwnerChanged(): Flow<Unit> {
        return nftRepository.getPendingSendTransactionsNftIds()
            .flatMapLatest(::subscribeNftsOwnerAddresses)
            .onEach {
                val blockTimeInMillis = chainStateRepository.expectedBlockTimeInMillis(it.chainId)
                delay(blockTimeInMillis.toLong())
                syncNftsList()
            }.map {}
    }

    private suspend fun subscribeNftsOwnerAddresses(nftIds: Set<String>): Flow<NftLocal> {
        val myAccountAddress = accountRepository.getSelectedMetaAccount().substrateAccountId?.toHexString()
        return nftIds.map { nftId ->
            val nftLocal = nftRepository.getLocalNft(nftId)
            nftRepository.subscribeNftOwnerAddress(nftLocal)
                .distinctUntilChanged()
                .filter { ownerAddress -> myAccountAddress != ownerAddress }
                .map { nftLocal }
        }
            .merge()
    }
}
