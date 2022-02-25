package io.novafoundation.nova.feature_nft_impl.data.source.providers.uniques

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.common.data.network.runtime.binding.getTyped
import io.novafoundation.nova.common.utils.uniques
import io.novafoundation.nova.core_db.dao.NftDao
import io.novafoundation.nova.core_db.model.NftLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_impl.data.network.distributed.FileStorageAdapter.adoptFileStorageLinkToHttps
import io.novafoundation.nova.feature_nft_impl.data.source.NftProvider
import io.novafoundation.nova.feature_nft_impl.data.source.providers.uniques.network.IpfsApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.Struct
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import java.math.BigInteger

class UniquesNftProvider(
    private val remoteStorage: StorageDataSource,
    private val nftDao: NftDao,
    private val ipfsApi: IpfsApi,
) : NftProvider {

    override suspend fun initialNftsSync(chain: Chain, metaAccount: MetaAccount) {
        val accountId = metaAccount.accountIdIn(chain)!!

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
                .mapValues { (_, parsedValue) ->
                    parsedValue?.cast<Struct.Instance>()?.getTyped<ByteArray>("data")
                }

            val totalIssuances = multiQueryResults.getValue(classStorage)
                .mapKeys { (keyComponents, _) -> keyComponents.component1<BigInteger>() }
                .mapValues { (_, parsedValue) ->
                    bindNumber(parsedValue.cast<Struct.Instance>()["instances"])
                }

            val instancesMetadatas = multiQueryResults.getValue(instanceMetadataStorage)
                .mapKeys { (keyComponents, _) -> keyComponents.component1<BigInteger>() to keyComponents.component2<BigInteger>() }
                .mapValues { (_, parsedValue) ->
                    parsedValue?.cast<Struct.Instance>()?.getTyped<ByteArray>("data")
                }

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

        nftDao.insertNftsDiff(NftLocal.Type.UNIQUES, metaAccount.id, newNfts)
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

    private fun identifier(chainId: ChainId, collectionId: BigInteger, instanceId: BigInteger): String {
        return "$chainId-$collectionId-$instanceId"
    }
}
