package io.novafoundation.nova.feature_nft_impl.data.repository

import android.util.Log
import io.novafoundation.nova.common.data.network.HttpExceptionHandler
import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindBlockNumber
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.core_db.dao.NftDao
import io.novafoundation.nova.core_db.model.NftLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_api.data.model.NftDetails
import io.novafoundation.nova.feature_nft_api.data.repository.NftRepository
import io.novafoundation.nova.feature_nft_impl.data.mappers.mapNftLocalToNft
import io.novafoundation.nova.feature_nft_impl.data.mappers.mapNftTypeLocalToTypeKey
import io.novafoundation.nova.feature_nft_impl.data.source.JobOrchestrator
import io.novafoundation.nova.feature_nft_impl.data.source.NftProvidersRegistry
import io.novafoundation.nova.feature_nft_impl.data.source.NftTransfersRegistry
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilder
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.ethereum.subscribe
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.math.BigInteger
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.suspendCoroutine

private const val NFT_TAG = "NFT"

class NftRepositoryImpl(
    private val nftProvidersRegistry: NftProvidersRegistry,
    private val chainRegistry: ChainRegistry,
    private val jobOrchestrator: JobOrchestrator,
    private val nftDao: NftDao,
    private val exceptionHandler: HttpExceptionHandler,
    private val storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
    private val nftTransfersRegistry: NftTransfersRegistry
) : NftRepository {

    private val mutex = Mutex()
    private val subscriptionBuilders: MutableMap<ChainId, StorageSharedRequestsBuilder> = mutableMapOf()

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

    override suspend fun subscribeNftOwnerAccountId(nftId: String): Flow<Pair<AccountId?, NftLocal>> {
        val nftLocal = getLocalNft(nftId)
        mutex.withLock {
            var isNewSubscriptionBuilder = false
            val subscriptionBuilder = subscriptionBuilders.getOrPut(nftLocal.chainId) {
                isNewSubscriptionBuilder = true
                storageSharedRequestsBuilderFactory.create(nftLocal.chainId)
            }
            val nftTypeKey = mapNftTypeLocalToTypeKey(nftDao.getNftType(nftLocal.identifier))
            val nftProvider = nftProvidersRegistry.get(nftTypeKey)
            return nftProvider.subscribeNftOwnerAccountId(
                subscriptionBuilder,
                nftLocal
            )
                .onStart {
                    if (isNewSubscriptionBuilder) {
                        subscriptionBuilder.subscribe(coroutineContext)
                    }
                }
                .map { address -> address to nftLocal }
        }
    }

    override suspend fun getLocalNft(nftIdentifier: String): NftLocal {
        return nftDao.getNft(nftIdentifier)
    }

    override suspend fun getLocalNfts(nftIdentifiers: List<String>): List<NftLocal> {
        return nftDao.getNfts(nftIdentifiers)
    }

    override suspend fun initialNftSync(
        metaAccount: MetaAccount,
        forceOverwrite: Boolean
    ): Unit = withContext(Dispatchers.IO) {
        val chains = chainRegistry.currentChains.first()

        val syncJobs = chains.flatMap { chain ->
            initialNftSyncForChain(chain, metaAccount, forceOverwrite, skipFirstBlock = false)
        }

        syncJobs.joinAll()
    }

    override suspend fun initialNftSyncForChainId(
        chainId: ChainId,
        metaAccount: MetaAccount,
        forceOverwrite: Boolean,
        skipFirstBlock: Boolean
    ): List<Job> {
        val chain = chainRegistry.getChain(chainId)
        return initialNftSyncForChain(chain, metaAccount, forceOverwrite, skipFirstBlock)
    }

    private suspend fun initialNftSyncForChain(
        chain: Chain,
        metaAccount: MetaAccount,
        forceOverwrite: Boolean,
        skipFirstBlock: Boolean
    ): List<Job> {
        return coroutineScope {
            var blockHash: BlockHash? = null
            if (skipFirstBlock) {
                blockHash = getNextBlockHash(chainId = chain.id)
            }
            nftProvidersRegistry.get(chain).map { nftProvider ->
                // launch separate job per each nftProvider
                launch {
                    // prevent whole sync from failing if some particular provider fails
                    runCatching {
                        nftProvider.initialNftsSync(chain, metaAccount, forceOverwrite, at = blockHash)
                    }.onFailure {
                        Log.e(NFT_TAG, "Failed to sync nfts in ${chain.name} using ${nftProvider::class.simpleName}", it)
                    }
                }
            }
        }
    }

    private suspend fun getNextBlockHash(chainId: ChainId): String {
        return coroutineScope {
            val subscriptionBuilder = storageSharedRequestsBuilderFactory.create(chainId)
            val runtime = chainRegistry.getRuntime(chainId)
            val storage = runtime.metadata.system().storage("Number")
            val blockNumberKey = storage.storageKey()
            val blocksFlow = subscriptionBuilder.subscribe(blockNumberKey)
            subscriptionBuilder.subscribe(this)
            var previousBlockNumber: BlockNumber = BigInteger.ZERO
            var currentBlockNumber: BlockNumber = BigInteger.ZERO
            blocksFlow
                .onEach {
                    previousBlockNumber = currentBlockNumber
                    currentBlockNumber = bindBlockNumber(it.value.orEmpty(), runtime)
                }
                .filter { currentBlockNumber > previousBlockNumber }
                .drop(1)
                .first()
                .block
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

    override suspend fun getAvailableChains(): List<Chain> {
        val chains = chainRegistry.currentChains.first()
        return chains.mapNotNull { chain ->
            chain.takeIf {
                nftTransfersRegistry.getAllTransfers().any {
                    it.areTransfersSupported(chain)
                }
            }
        }
    }

    override fun isNftTypeSupportedForSend(nftType: Nft.Type, chain: Chain): Boolean {
        return nftTransfersRegistry.get(nftType.key).areTransfersSupported(chain)
    }

    override suspend fun getChainForNftId(nftId: String): Chain {
        val chainId = nftDao.getNft(nftId).chainId
        return chainRegistry.getChain(chainId)
    }
}
