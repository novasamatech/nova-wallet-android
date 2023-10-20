package io.novafoundation.nova.feature_nft_impl.data.source.providers.nfts

import com.google.gson.Gson
import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.common.data.network.runtime.binding.UseCaseBinding
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountId
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.common.data.network.runtime.binding.getTyped
import io.novafoundation.nova.common.data.network.runtime.binding.returnType
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.nfts
import io.novafoundation.nova.core_db.dao.NftDao
import io.novafoundation.nova.core_db.model.NftLocal
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_api.data.model.NftDetails
import io.novafoundation.nova.feature_nft_impl.data.mappers.mapNftLocalToNftType
import io.novafoundation.nova.feature_nft_impl.data.mappers.nftIssuance
import io.novafoundation.nova.feature_nft_impl.data.network.distributed.FileStorageAdapter.adoptFileStorageLinkToHttps
import io.novafoundation.nova.feature_nft_impl.data.source.NftProvider
import io.novafoundation.nova.feature_nft_impl.data.source.providers.common.MetadataLimits
import io.novafoundation.nova.feature_nft_impl.data.source.providers.common.mapJsonToAttributes
import io.novafoundation.nova.feature_nft_impl.data.source.providers.common.network.IpfsApi
import io.novafoundation.nova.feature_nft_impl.data.source.providers.common.network.UniquesNftsMetadata
import io.novafoundation.nova.feature_nft_impl.domain.common.mapNftNameForUi
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilder
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.singleValueOf
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

class NftsNftProvider(
    private val remoteStorage: StorageDataSource,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
    private val nftDao: NftDao,
    private val ipfsApi: IpfsApi,
    private val gson: Gson
) : NftProvider {

    override suspend fun initialNftsSync(
        chain: Chain,
        metaAccount: MetaAccount,
        forceOverwrite: Boolean,
        at: BlockHash?
    ) {
        val accountId = metaAccount.accountIdIn(chain) ?: return

        val newNfts = remoteStorage.query(chain.id) {
            val classesWithInstances = runtime.metadata.nfts().storage("Account").keys(accountId)
                .map { (_: AccountId, collection: BigInteger, instance: BigInteger) ->
                    listOf(collection, instance)
                }

            val classesIds = classesWithInstances.map { (collection, _) -> collection }.distinct()

            val classMetadataStorage = runtime.metadata.nfts().storage("CollectionMetadataOf")
            val instanceMetadataStorage = runtime.metadata.nfts().storage("ItemMetadataOf")
            val classStorage = runtime.metadata.nfts().storage("Collection")

            val multiQueryResults = multi {
                classStorage.querySingleArgKeys(classesIds)
                classMetadataStorage.querySingleArgKeys(classesIds)
                instanceMetadataStorage.queryKeys(classesWithInstances)
            }

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

                val metadata = instancesMetadatas[instanceKey]

                NftLocal(
                    identifier = identifier(chain.id, collectionId, instanceId),
                    metaId = metaAccount.id,
                    chainId = chain.id,
                    collectionId = collectionId.toString(),
                    instanceId = instanceId.toString(),
                    metadata = metadata,
                    type = NftLocal.Type.NFTS,
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

        nftDao.insertNftsDiff(NftLocal.Type.NFTS, metaAccount.id, newNfts, forceOverwrite)
    }

    override suspend fun getCollectionNameAndMedia(
        collectionId: String,
        chainId: ChainId?
    ): Pair<String?, String?>? {
        if (chainId == null) return null

        val classId = collectionId.toBigInteger()
        return remoteStorage.query(chainId) {
            val classMetadataStorage = runtime.metadata.nfts().storage("CollectionMetadataOf")
            val classStorage = runtime.metadata.nfts().storage("Collection")

            val queryResults = multi {
                classMetadataStorage.queryKey(classId)
                classStorage.queryKey(classId)
            }
            val classMetadataPointer = bindMetadata(queryResults.singleValueOf(classMetadataStorage))

            classMetadataPointer?.let {
                val url = classMetadataPointer.decodeToString().adoptFileStorageLinkToHttps()
                val classMetadata = ipfsApi.getIpfsMetadata(url)

                Pair(
                    classMetadata.name?.take(MetadataLimits.COLLECTION_NAME_LIMIT),
                    classMetadata.image?.adoptFileStorageLinkToHttps(),
                )
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

        val metadata = runCatching {
            ipfsApi.getIpfsMetadata(metadataLink)
        }.getOrDefault(UniquesNftsMetadata.Default).run {
            copy(
                name = name?.take(MetadataLimits.NFT_NAME_LIMIT),
                description = description?.take(MetadataLimits.DESCRIPTION_LIMIT),
                tags = tags?.take(MetadataLimits.TAGS_LIMIT),
                attributes = attributes?.take(MetadataLimits.ATTRIBUTES_LIMIT)
            )
        }

        nftDao.updateNft(identifier) { local ->
            local.copy(
                name = metadata.name ?: local.name,
                media = metadata.image?.adoptFileStorageLinkToHttps() ?: local.media,
                label = metadata.description ?: local.label,
                tags = metadata.tags?.let { gson.toJson(it) } ?: local.tags,
                attributes = metadata.attributes?.let { gson.toJson(it) } ?: local.attributes,
                wholeDetailsLoaded = true
            )
        }
    }

    override suspend fun subscribeNftOwnerAccountId(
        subscriptionBuilder: StorageSharedRequestsBuilder,
        nftLocal: NftLocal
    ): Flow<AccountId?> {
        return remoteStorage.query(nftLocal.chainId) {
            val storage = runtime.metadata.nfts().storage("Item")
            val key = storage.storageKey(
                runtime,
                nftLocal.collectionId.toBigInteger(),
                nftLocal.instanceId?.toBigInteger()
            )
            try {
                subscriptionBuilder.subscribe(key)
                    .map { bindNftOwnerAccountId(it.value, runtime) }
                    .flowOn(Dispatchers.IO)
            } catch (e: Exception) {
                emptyFlow()
            }
        }
    }

    override fun nftDetailsFlow(nftIdentifier: String): Flow<NftDetails> {
        return flowOf {
            val notSyncedNftLocal = nftDao.getNft(nftIdentifier)

            nftFullSync(notSyncedNftLocal.metadata, notSyncedNftLocal.identifier)
            val syncedNftLocal = nftDao.getNft(nftIdentifier)

            val chain = chainRegistry.getChain(syncedNftLocal.chainId)
            val metaAccount = accountRepository.getMetaAccount(syncedNftLocal.metaId)

            val classId = syncedNftLocal.collectionId.toBigInteger()

            remoteStorage.query(chain.id) {
                val classMetadataStorage = runtime.metadata.nfts().storage("CollectionMetadataOf")
                val classStorage = runtime.metadata.nfts().storage("Collection")

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

                NftDetails(
                    identifier = syncedNftLocal.identifier,
                    chain = chain,
                    owner = metaAccount.requireAccountIdIn(chain),
                    creator = null,
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

    private fun bindNftOwnerAccountId(scale: String?, runtime: RuntimeSnapshot): AccountId? {
        return scale?.let { bindOrmlAccountData(it, runtime) }
    }

    @UseCaseBinding
    private fun bindOrmlAccountData(scale: String, runtime: RuntimeSnapshot): AccountId {
        val type = runtime.metadata.nfts().storage("Item").returnType()

        val dynamicInstance = type.fromHexOrNull(runtime, scale).cast<Struct.Instance>()

        return bindAccountId(dynamicInstance["owner"])
    }
}
