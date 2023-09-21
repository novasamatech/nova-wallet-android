package io.novafoundation.nova.feature_nft_impl.data.repository

import android.util.Log
import io.novafoundation.nova.common.data.network.HttpExceptionHandler
import io.novafoundation.nova.common.utils.flowOf
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
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.ethereum.subscribe
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

private const val NFT_TAG = "NFT"

class NftRepositoryImpl(
    private val nftProvidersRegistry: NftProvidersRegistry,
    private val chainRegistry: ChainRegistry,
    private val jobOrchestrator: JobOrchestrator,
    private val nftDao: NftDao,
    private val exceptionHandler: HttpExceptionHandler,
    private val storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory
) : NftRepository {

    private val collectionNameAndMedias: MutableMap<String, Pair<String?, String?>?> = mutableMapOf()

    override fun allNftWithMetadataFlow(metaAccount: MetaAccount): Flow<List<Nft>> {
        return nftDao.nftsFlow(metaAccount.id)
            .map { nftsLocal ->
                val chainsById = chainRegistry.chainsById.first()

                nftsLocal.mapNotNull { nftLocal ->
                    val (collectionName, collectionMedia) = getCollectionNameAndMedia(nftLocal, chainsById) ?: Pair(null, null)
                    mapNftLocalToNft(chainsById, metaAccount, nftLocal, collectionName, collectionMedia)
                }
            }
    }

    private suspend fun getCollectionNameAndMedia(
        nftLocal: NftLocal,
        chainsById: Map<ChainId, Chain>
    ): Pair<String?, String?>? {
        val nftTypeKey = mapNftTypeLocalToTypeKey(nftLocal.type)
        val nftProvider = nftProvidersRegistry.get(nftTypeKey)
        val chain = chainsById[nftLocal.chainId]
        val collectionId = nftLocal.collectionId
        return runCatching {
            if (collectionNameAndMedias.containsKey(collectionId)) {
                collectionNameAndMedias[collectionId]
            } else {
                nftProvider.getCollectionNameAndMedia(collectionId, chain?.id).apply {
                    collectionNameAndMedias[collectionId] = this
                }
            }
        }.getOrDefault(null)
    }

    override fun allNftFlow(metaAccount: MetaAccount): Flow<List<Nft>> {
        return nftDao.nftsFlow(metaAccount.id)
            .map { nftsLocal ->
                val chainsById = chainRegistry.chainsById.first()

                nftsLocal.mapNotNull { nftLocal ->
                    mapNftLocalToNft(chainsById, metaAccount, nftLocal, null, null)
                }
            }
    }

    override fun nftDetails(nftId: String): Flow<NftDetails> {
        return flow {
            val nftType = nftDao.getNftType(nftId)
            val nftTypeKey = mapNftTypeLocalToTypeKey(nftType)
            val nftProvider = nftProvidersRegistry.get(nftTypeKey)

            emitAll(nftProvider.nftDetailsFlow(nftId))
        }.catch { throw exceptionHandler.transformException(it) }
    }

    override fun subscribeNftOwnerAddress(nftLocal: NftLocal): Flow<String> {
        return flowOf {
            val subscriptionBuilder = storageSharedRequestsBuilderFactory.create(nftLocal.chainId)
            nftLocal to subscriptionBuilder
        }.flatMapConcat { (nftLocal, subscriptionBuilder) ->
            val nftTypeKey = mapNftTypeLocalToTypeKey(nftLocal.type)
            val nftProvider = nftProvidersRegistry.get(nftTypeKey)
            nftProvider.subscribeNftOwnerAddress(
                subscriptionBuilder,
                nftLocal
            ).onStart { subscriptionBuilder.subscribe(coroutineContext) }
        }
    }

    override suspend fun getLocalNft(nftIdentifier: String): NftLocal {
        return nftDao.getNft(nftIdentifier)
    }

    override suspend fun getLocalNftOrNull(nftIdentifier: String): NftLocal? {
        return nftDao.getNftOrNull(nftIdentifier)
    }

    override suspend fun initialNftSync(
        metaAccount: MetaAccount,
        forceOverwrite: Boolean,
    ): Unit = withContext(Dispatchers.IO) {
        val chains = chainRegistry.currentChains.first()

        val syncJobs = chains.flatMap { chain ->
            nftProvidersRegistry.get(chain).map { nftProvider ->
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

        syncJobs.joinAll()
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
            chain.takeIf { nftProvidersRegistry.isAvailableChain(chain) }
        }
    }

    override fun onNftSendTransactionSubmitted(nftLocal: NftLocal) {
        pendingSendTransactionsNftLocals.value = pendingSendTransactionsNftLocals.value.toMutableSet().apply {
            add(nftLocal)
        }
    }

    override fun removeOldPendingTransactions(nftLocal: NftLocal) {
        pendingSendTransactionsNftLocals.value = pendingSendTransactionsNftLocals.value.toMutableSet().apply {
            toList().forEach { _ -> removeIf { it.identifier == nftLocal.identifier } }
        }
    }

    override fun getPendingSendTransactionsNftLocals(): Flow<Set<NftLocal>> {
        return pendingSendTransactionsNftLocals.asStateFlow()
    }

    override fun isNftTypeSupportedForSend(nftType: Nft.Type): Boolean {
        return nftType::class in supportedSendNftTypes
    }

    companion object {
        private val supportedSendNftTypes = setOf(Nft.Type.Uniques::class)
        private val pendingSendTransactionsNftLocals = MutableStateFlow(setOf<NftLocal>())
    }
}
