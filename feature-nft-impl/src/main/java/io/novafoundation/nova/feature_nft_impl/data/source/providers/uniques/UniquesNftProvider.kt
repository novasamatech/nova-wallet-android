package io.novafoundation.nova.feature_nft_impl.data.source.providers.uniques

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountId
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.common.data.network.runtime.binding.getTyped
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.uniques
import io.novafoundation.nova.core_db.dao.NftDao
import io.novafoundation.nova.core_db.model.NftLocal
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_api.data.model.NftDetails
import io.novafoundation.nova.feature_nft_impl.data.mappers.nftIssuance
import io.novafoundation.nova.feature_nft_impl.data.network.distributed.FileStorageAdapter.adoptFileStorageLinkToHttps
import io.novafoundation.nova.feature_nft_impl.data.source.NftProvider
import io.novafoundation.nova.feature_nft_impl.data.source.providers.uniques.network.IpfsApi
import io.novafoundation.nova.runtime.ext.isFullSync
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.singleValueOf
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.Struct
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import kotlinx.coroutines.flow.Flow
import java.math.BigInteger

class UniquesNftProvider(
    private val remoteStorage: StorageDataSource,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
    private val nftDao: NftDao,
    private val ipfsApi: IpfsApi,
) : NftProvider {

    override val requireFullChainSync: Boolean = true

    override suspend fun initialNftsSync(chain: Chain, metaAccount: MetaAccount, forceOverwrite: Boolean) {
        if (chain.connectionState.isFullSync) return
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

                    wholeDetailsLoaded = false
                )
            }
        }

        nftDao.insertNftsDiff(NftLocal.Type.UNIQUES, metaAccount.id, newNfts, forceOverwrite)
    }

    override suspend fun nftFullSync(nft: Nft) {
        if (nft.metadataRaw == null) {
            nftDao.markFullSynced(nft.identifier)

            return
        }

        val metadataLink = nft.metadataRaw!!.decodeToString().adoptFileStorageLinkToHttps()
        val metadata = ipfsApi.getIpfsMetadata(metadataLink)

        nftDao.updateNft(nft.identifier) { local ->
            local.copy(
                name = metadata.name!!,
                media = metadata.image?.adoptFileStorageLinkToHttps(),
                label = metadata.description,
                wholeDetailsLoaded = true
            )
        }
    }

    override fun nftDetailsFlow(nftIdentifier: String): Flow<NftDetails> {
        return flowOf {
            val nftLocal = nftDao.getNft(nftIdentifier)
            require(nftLocal.wholeDetailsLoaded) {
                "Cannot load details of non fully-synced NFT"
            }

            val chain = chainRegistry.getChain(nftLocal.chainId)
            val metaAccount = accountRepository.getMetaAccount(nftLocal.metaId)

            val classId = nftLocal.collectionId.toBigInteger()

            remoteStorage.query(chain.id) {
                val classMetadataStorage = runtime.metadata.uniques().storage("ClassMetadataOf")
                val classStorage = runtime.metadata.uniques().storage("Class")

                val queryResults = multi {
                    classMetadataStorage.queryKey(classId)
                    classStorage.queryKey(classId)
                }
                val classMetadataPointer = bindMetadata(queryResults.singleValueOf(classMetadataStorage))

                val collection = if (classMetadataPointer == null) {
                    NftDetails.Collection(nftLocal.collectionId)
                } else {
                    val url = classMetadataPointer.decodeToString().adoptFileStorageLinkToHttps()
                    val classMetadata = ipfsApi.getIpfsMetadata(url)

                    NftDetails.Collection(
                        id = nftLocal.collectionId,
                        name = classMetadata.name,
                        media = classMetadata.image?.adoptFileStorageLinkToHttps()
                    )
                }

                val classIssuerRaw = queryResults.singleValueOf(classStorage)
                val classIssuer = bindAccountId(classIssuerRaw.cast<Struct.Instance>()["issuer"])

                NftDetails(
                    identifier = nftLocal.identifier,
                    chain = chain,
                    owner = metaAccount.accountIdIn(chain)!!,
                    creator = classIssuer,
                    media = nftLocal.media,
                    name = nftLocal.name ?: nftLocal.instanceId!!,
                    description = nftLocal.label,
                    issuance = nftIssuance(nftLocal),
                    price = nftLocal.price,
                    collection = collection
                )
            }
        }
    }

    private fun bindMetadata(dynamic: Any?): ByteArray? = dynamic?.cast<Struct.Instance>()?.getTyped("data")

    private fun identifier(chainId: ChainId, collectionId: BigInteger, instanceId: BigInteger): String {
        return "$chainId-$collectionId-$instanceId"
    }
}
