package io.novafoundation.nova.feature_nft_impl.data.repository

import android.util.Log
import io.novafoundation.nova.core_db.dao.NftDao
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_api.data.repository.NftRepository
import io.novafoundation.nova.feature_nft_impl.data.mappers.mapNftLocalToNft
import io.novafoundation.nova.feature_nft_impl.data.source.NftProvidersRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val NFT_TAG = "NFT"

class NftRepositoryImpl(
    private val nftProvidersRegistry: NftProvidersRegistry,
    private val chainRegistry: ChainRegistry,
    private val nftDao: NftDao,
) : NftRepository {

    override fun allNftFlow(metaAccount: MetaAccount): Flow<List<Nft>> {
        return nftDao.nftsFlow(metaAccount.id)
            .map { nftsLocal ->
                val chainsById = chainRegistry.chainsById.first()

                nftsLocal.mapNotNull { nftLocal ->
                    mapNftLocalToNft(chainsById, metaAccount, nftLocal)
                }
            }
    }

    override suspend fun initialNftSync(metaAccount: MetaAccount): Unit = withContext(Dispatchers.IO) {
        val chains = chainRegistry.currentChains.first()

        val syncJobs = chains.flatMap { chain ->
            nftProvidersRegistry.get(chain).map { nftProvider ->
                // launch separate job per each nftProvider
                launch {
                    // prevent whole sync from failing if some particular provider fails
                    runCatching {
                        nftProvider.initialNftsSync(chain, metaAccount)
                    }.onFailure {
                        Log.e(NFT_TAG, "Failed to sync nfts in ${chain.name} using ${nftProvider::class.simpleName}", it)
                    }
                }
            }
        }

        syncJobs.joinAll()
    }

    override suspend fun fullNftSync(nft: Nft) {
        TODO("Not yet implemented")
    }
}
