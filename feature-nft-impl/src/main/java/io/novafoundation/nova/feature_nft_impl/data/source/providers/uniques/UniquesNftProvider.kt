package io.novafoundation.nova.feature_nft_impl.data.source.providers.uniques

import com.google.gson.Gson
import io.novafoundation.nova.common.data.network.runtime.binding.UseCaseBinding
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountId
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.common.data.network.runtime.binding.getTyped
import io.novafoundation.nova.common.data.network.runtime.binding.returnType
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.uniques
import io.novafoundation.nova.core_db.dao.NftDao
import io.novafoundation.nova.core_db.model.NftLocal
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_api.data.model.NftDetails
import io.novafoundation.nova.feature_nft_impl.data.mappers.mapNftLocalToNftType
import io.novafoundation.nova.feature_nft_impl.data.mappers.nftIssuance
import io.novafoundation.nova.feature_nft_impl.data.network.distributed.FileStorageAdapter.adoptFileStorageLinkToHttps
import io.novafoundation.nova.feature_nft_impl.data.source.NftProvider
import io.novafoundation.nova.feature_nft_impl.data.source.providers.common.mapJsonToAttributes
import io.novafoundation.nova.feature_nft_impl.data.source.providers.uniques.network.IpfsApi
import io.novafoundation.nova.feature_nft_impl.domain.common.mapNftNameForUi
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilder
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.singleValueOf
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.Struct
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHexOrNull
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.math.BigInteger

class UniquesNftProvider(
    private val remoteStorage: StorageDataSource,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
    private val nftDao: NftDao,
    private val ipfsApi: IpfsApi,
    private val gson: Gson
) : NftProvider {

    override suspend fun initialNftsSync(chain: Chain, metaAccount: MetaAccount, forceOverwrite: Boolean) {
        val accountId = metaAccount.accountIdIn(chain) ?: return

        val newNfts = remoteStorage.query(chain.id) {
            val classesWithInstances = runtime.metadata.uniques().storage("Account").keys(accountId)
                .map { (_: AccountId, collection: BigInteger, instance: BigInteger) ->
                    listOf(collection, instance)
                }

            val classesIds = classesWithInstances.map { (collection, _) -> collection }.distinct()

            val classMetadataStorage = runtime.metadata.uniques().storage("ClassMetadataOf")
            val instanceMetadataStorage = runtime.metadata.uniques().storage("InstanceMetadataOf")
            val classStorage = runtime.metadata.uniques().storage("Class")

            val multiQueryResults = multi {
                classStorage.querySingleArgKeys(classesIds)
                classMetadataStorage.querySingleArgKeys(classesIds)
                instanceMetadataStorage.queryKeys(classesWithInstances)
            }

            val classMetadatas = multiQueryResults.getValue(classMetadataStorage)
                .mapKeys { (keyComponents, _) -> keyComponents.component1<BigInteger>() }
                .mapValues { (_, parsedValue) -> bindMetadata(parsedValue) }

            val totalIssuances = multiQueryResults.getValue(classStorage)
                .mapKeys { (keyComponents, _) -> keyComponents.component1<BigInteger>() }
                .mapValues { (_, parsedValue) ->
                    bindNumber(parsedValue.cast<Struct.Instance>()["items"])
                }

            val instancesMetadatas = multiQueryResults.getValue(instanceMetadataStorage)
                .mapKeys { (keyComponents, _) -> keyComponents.component1<BigInteger>() to keyComponents.component2<BigInteger>() }
                .mapValues { (_, parsedValue) -> bindMetadata(parsedValue) }

            classesWithInstances.map { (collectionId, instanceId) ->
                val instanceKey = collectionId to instanceId

                val metadata = instancesMetadatas[instanceKey] ?: classMetadatas[collectionId]

                NftLocal(
                    identifier = identifier(chain.id, collectionId, instanceId),
                    metaId = metaAccount.id,
                    chainId = chain.id,
                    collectionId = collectionId.toString(),
                    instanceId = instanceId.toString(),
                    metadata = metadata,
                    type = NftLocal.Type.UNIQUES,
                    issuanceTotal = totalIssuances.getValue(collectionId).toInt(),
                    issuanceMyEdition = instanceId.toString(),
                    price = null,

                    // to load at full sync
                    name = null,
                    label = null,
                    media = null,
                    tags = null,

                    wholeDetailsLoaded = false
                )
            }
        }

        nftDao.insertNftsDiff(NftLocal.Type.UNIQUES, metaAccount.id, newNfts, forceOverwrite)
    }

    override suspend fun getCollectionName(
        collectionId: String,
        chainId: ChainId?
    ): String? {
        if (chainId == null) return null

        val classId = collectionId.toBigInteger()
        return remoteStorage.query(chainId) {
            val classMetadataStorage = runtime.metadata.uniques().storage("ClassMetadataOf")
            val classStorage = runtime.metadata.uniques().storage("Class")

            val queryResults = multi {
                classMetadataStorage.queryKey(classId)
                classStorage.queryKey(classId)
            }
            val classMetadataPointer = bindMetadata(queryResults.singleValueOf(classMetadataStorage))

            classMetadataPointer?.let {
                val url = classMetadataPointer.decodeToString().adoptFileStorageLinkToHttps()
                val classMetadata = ipfsApi.getIpfsMetadata(url)

                classMetadata.name
            }
        }
    }

    override suspend fun nftFullSync(nft: Nft) {
        nftFullSync(nft.metadataRaw, nft.identifier)
    }

    private suspend fun nftFullSync(
        metadataRaw: ByteArray?,
        identifier: String
    ) {
        if (metadataRaw == null) {
            nftDao.markFullSynced(identifier)
            return
        }

        val metadataLink = metadataRaw.decodeToString().adoptFileStorageLinkToHttps()
        val metadata = ipfsApi.getIpfsMetadata(metadataLink)

        nftDao.updateNft(identifier) { local ->
            local.copy(
                name = metadata.name!!,
                media = metadata.image?.adoptFileStorageLinkToHttps(),
                label = metadata.description,
                tags = metadata.tags?.let { gson.toJson(it) },
                attributes = metadata.attributes?.let { gson.toJson(it) },
                wholeDetailsLoaded = true
            )
        }
    }

    override suspend fun subscribeNftOwnerAddress(
        subscriptionBuilder: StorageSharedRequestsBuilder,
        nftLocal: NftLocal
    ): Flow<String> {
        return remoteStorage.query(nftLocal.chainId) {
            val storage = runtime.metadata.uniques().storage("Asset")
            val key = storage.storageKey(
                runtime,
                nftLocal.collectionId.toBigInteger(),
                nftLocal.instanceId?.toBigInteger()
            )
            try {
                subscriptionBuilder.subscribe(key)
                    .map { bindNftOwnerAddress(it.value, runtime) }
                    .flowOn(Dispatchers.IO)
            } catch (e: Exception) {
                println("error: $e")
                emptyFlow()
            }
        }
    }

    override fun nftDetailsFlow(nftIdentifier: String): Flow<NftDetails> {
        return flowOf {
            val notSyncedNftLocal = nftDao.getNft(nftIdentifier)
            require(notSyncedNftLocal.wholeDetailsLoaded) {
                "Cannot load details of non fully-synced NFT"
            }

            nftFullSync(notSyncedNftLocal.metadata, notSyncedNftLocal.identifier)
            val syncedNftLocal = nftDao.getNft(nftIdentifier)

            val chain = chainRegistry.getChain(syncedNftLocal.chainId)
            val metaAccount = accountRepository.getMetaAccount(syncedNftLocal.metaId)

            val classId = syncedNftLocal.collectionId.toBigInteger()

            remoteStorage.query(chain.id) {
                val classMetadataStorage = runtime.metadata.uniques().storage("ClassMetadataOf")
                val classStorage = runtime.metadata.uniques().storage("Class")

                val queryResults = multi {
                    classMetadataStorage.queryKey(classId)
                    classStorage.queryKey(classId)
                }
                val classMetadataPointer = bindMetadata(queryResults.singleValueOf(classMetadataStorage))

                val collection = if (classMetadataPointer == null) {
                    NftDetails.Collection(syncedNftLocal.collectionId)
                } else {
                    val url = classMetadataPointer.decodeToString().adoptFileStorageLinkToHttps()
                    val classMetadata = ipfsApi.getIpfsMetadata(url)

                    NftDetails.Collection(
                        id = syncedNftLocal.collectionId,
                        name = classMetadata.name,
                        media = classMetadata.image?.adoptFileStorageLinkToHttps()
                    )
                }

                val classIssuerRaw = queryResults.singleValueOf(classStorage)
                val classIssuer = bindAccountId(classIssuerRaw.cast<Struct.Instance>()["issuer"])

                NftDetails(
                    identifier = syncedNftLocal.identifier,
                    chain = chain,
                    owner = metaAccount.accountIdIn(chain)!!,
                    creator = classIssuer,
                    media = syncedNftLocal.media,
                    name = mapNftNameForUi(syncedNftLocal.name, syncedNftLocal.instanceId),
                    description = syncedNftLocal.label,
                    issuance = nftIssuance(syncedNftLocal),
                    price = syncedNftLocal.price,
                    collection = collection,
                    type = mapNftLocalToNftType(syncedNftLocal),
                    tags = if (syncedNftLocal.tags == null) {
                        emptyList()
                    } else {
                        gson.fromJson(syncedNftLocal.tags, List::class.java) as List<String>
                    },
                    attributes = mapJsonToAttributes(gson, syncedNftLocal.attributes),
                )
            }
        }
    }

    private fun bindMetadata(dynamic: Any?): ByteArray? = dynamic?.cast<Struct.Instance>()?.getTyped("data")

    private fun identifier(chainId: ChainId, collectionId: BigInteger, instanceId: BigInteger): String {
        return "$chainId-$collectionId-$instanceId"
    }
    
    private fun bindNftOwnerAddress(scale: String?, runtime: RuntimeSnapshot): String {
        return scale?.let { bindOrmlAccountData(it, runtime) } ?: ""
    }

    @UseCaseBinding
    fun bindOrmlAccountData(scale: String, runtime: RuntimeSnapshot): String {
        val type = runtime.metadata.uniques().storage("Asset").returnType()

        val dynamicInstance = type.fromHexOrNull(runtime, scale).cast<Struct.Instance>()

        return bindAccountId(dynamicInstance["owner"]).toHexString()
    }
}
