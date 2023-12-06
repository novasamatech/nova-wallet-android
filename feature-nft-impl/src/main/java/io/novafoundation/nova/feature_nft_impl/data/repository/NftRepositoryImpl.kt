package io.novafoundation.nova.feature_nft_impl.data.repository

import android.util.Log
import io.novafoundation.nova.common.data.network.HttpExceptionHandler
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.transformLatestDiffed
import io.novafoundation.nova.core_db.dao.NftDao
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_api.data.model.NftDetails
import io.novafoundation.nova.feature_nft_api.data.repository.NftRepository
import io.novafoundation.nova.feature_nft_api.data.repository.NftSyncTrigger
import io.novafoundation.nova.feature_nft_impl.data.mappers.mapNftLocalToNft
import io.novafoundation.nova.feature_nft_impl.data.mappers.mapNftTypeLocalToTypeKey
import io.novafoundation.nova.feature_nft_impl.data.source.JobOrchestrator
import io.novafoundation.nova.feature_nft_impl.data.source.NftProvider
import io.novafoundation.nova.feature_nft_impl.data.source.NftProvidersRegistry
import io.novafoundation.nova.runtime.ext.level
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val NFT_TAG = "NFT"

class NftRepositoryImpl(
    private val nftProvidersRegistry: NftProvidersRegistry,
    private val chainRegistry: ChainRegistry,
    private val jobOrchestrator: JobOrchestrator,
    private val nftDao: NftDao,
    private val exceptionHandler: HttpExceptionHandler,
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

    override fun nftDetails(nftId: String): Flow<NftDetails> {
        return flow {
            val nftTypeKey = mapNftTypeLocalToTypeKey(nftDao.getNftType(nftId))
            val nftProvider = nftProvidersRegistry.get(nftTypeKey)

            emitAll(nftProvider.nftDetailsFlow(nftId))
        }.catch { throw exceptionHandler.transformException(it) }
    }

    override fun initialNftSyncTrigger(): Flow<NftSyncTrigger> {
        return flowOfAll { chainRegistry.currentChains }
            .map { chains -> chains.filter { nftProvidersRegistry.nftSupported(it) } }
            .transformLatestDiffed { NftSyncTrigger(it) }
    }

    override suspend fun initialNftSync(
        metaAccount: MetaAccount,
        forceOverwrite: Boolean,
    ): Unit = withContext(Dispatchers.IO) {
        val chains = chainRegistry.currentChains.first()

        val syncJobs = chains.flatMap { chain ->
            nftSyncJobs(chain, metaAccount, forceOverwrite)
        }

        syncJobs.joinAll()
    }

    override suspend fun initialNftSync(metaAccount: MetaAccount, chain: Chain) = withContext(Dispatchers.IO) {
        val syncJobs = nftSyncJobs(chain, metaAccount, forceOverwrite = false)

        syncJobs.joinAll()
    }

    private fun CoroutineScope.nftSyncJobs(chain: Chain, metaAccount: MetaAccount, forceOverwrite: Boolean): List<Job> {
        return nftProvidersRegistry.get(chain)
            .filter { it.canSyncIn(chain) }
            .map { nftProvider ->
                // launch separate job per each nftProvider
                launch {
                    // prevent whole sync from failing if some particular provider fails
                    runCatching {
                        nftProvider.initialNftsSync(chain, metaAccount, forceOverwrite)
                    }.onFailure {
                        Log.e(NFT_TAG, "Failed to sync nfts in ${chain.name} using ${nftProvider::class.simpleName}", it)
                    }
                }
            }
    }

    override suspend fun fullNftSync(nft: Nft) = withContext(Dispatchers.IO) {
        jobOrchestrator.runUniqueJob(nft.identifier) {
            runCatching {
                nftProvidersRegistry.get(nft.type.key).nftFullSync(nft)
            }.onFailure {
                Log.e(NFT_TAG, "Failed to fully sync nft ${nft.identifier} in ${nft.chain.name} with type ${nft.type::class.simpleName}", it)
            }
        }
    }

    private fun NftProvider.canSyncIn(chain: Chain): Boolean {
        val requiredStage = if (requireFullChainSync) Chain.ConnectionState.FULL_SYNC else Chain.ConnectionState.LIGHT_SYNC

        return chain.connectionState.level >= requiredStage.level
    }
}
