package io.novafoundation.nova.feature_nft_impl.data.source.providers.rmrkV2

import com.google.gson.Gson
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.core_db.dao.NftDao
import io.novafoundation.nova.core_db.model.NftLocal
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_api.data.model.NftDetails
import io.novafoundation.nova.feature_nft_impl.data.mappers.mapNftLocalToNftType
import io.novafoundation.nova.feature_nft_impl.data.mappers.nftIssuance
import io.novafoundation.nova.feature_nft_impl.data.mappers.nftPrice
import io.novafoundation.nova.feature_nft_impl.data.network.distributed.FileStorageAdapter.adoptFileStorageLinkToHttps
import io.novafoundation.nova.feature_nft_impl.data.source.NftProvider
import io.novafoundation.nova.feature_nft_impl.data.source.providers.common.mapJsonToAttributes
import io.novafoundation.nova.feature_nft_impl.data.source.providers.rmrkV2.network.singular.SingularV2Api
import io.novafoundation.nova.feature_nft_impl.presentation.nft.common.mapNftNameForUi
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilder
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow

class RmrkV2NftProvider(
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val singularV2Api: SingularV2Api,
    private val nftDao: NftDao,
    private val gson: Gson
) : NftProvider {

    override suspend fun initialNftsSync(chain: Chain, metaAccount: MetaAccount, forceOverwrite: Boolean) {
        val address = metaAccount.addressIn(chain) ?: return
        val nfts = singularV2Api.getAccountNfts(address)

        val toSave = nfts.map {
            NftLocal(
                identifier = localIdentifier(chain.id, it.id),
                metaId = metaAccount.id,
                chainId = chain.id,
                collectionId = it.collectionId,
                instanceId = it.id,
                metadata = it.metadata?.encodeToByteArray(),
                media = it.image?.adoptFileStorageLinkToHttps(),

                // let name default to symbol and label to edition in case full sync wont be able to determine them from metadata
                name = it.symbol,
                label = it.edition,

                price = it.price,
                type = NftLocal.Type.RMRK2,
                issuanceMyEdition = it.edition,
                wholeDetailsLoaded = false,
            )
        }

        nftDao.insertNftsDiff(NftLocal.Type.RMRK2, metaAccount.id, toSave, forceOverwrite)
    }

    override suspend fun subscribeNftOwnerAddress(
        subscriptionBuilder: StorageSharedRequestsBuilder,
        nftLocal: NftLocal
    ): Flow<String> {
        throw UnsupportedOperationException("RmrkV2 doesn't supported")
    }

    override suspend fun nftFullSync(nft: Nft) {
        nftFullSync(
            nft.metadataRaw,
            nft.collectionId,
            nft.identifier
        )
    }

    private suspend fun nftFullSync(
        metadataRaw: ByteArray?,
        collectionId: String,
        identifier: String
    ) {
        val metadata = metadataRaw?.let {
            val metadataLink = it.decodeToString().adoptFileStorageLinkToHttps()

            singularV2Api.getIpfsMetadata(metadataLink)
        }

        val collection = singularV2Api.getCollection(collectionId).first()

        nftDao.updateNft(identifier) { local ->
            // media fetched during initial sync (prerender) has more priority than one from metadata
            val image = local.media ?: metadata?.image?.adoptFileStorageLinkToHttps()

            local.copy(
                media = image,
                issuanceTotal = collection.max,
                name = metadata?.name ?: local.name,
                label = metadata?.description ?: local.label,
                wholeDetailsLoaded = true,
                tags = metadata?.tags?.let { gson.toJson(it) },
                attributes = metadata?.attributes?.let { gson.toJson(it) }
            )
        }
    }

    override fun nftDetailsFlow(nftIdentifier: String): Flow<NftDetails> {
        return flowOf {
            val notSyncedNftLocal = nftDao.getNft(nftIdentifier)
            require(notSyncedNftLocal.wholeDetailsLoaded) {
                "Cannot load details of non fully-synced NFT"
            }

            nftFullSync(
                notSyncedNftLocal.metadata,
                notSyncedNftLocal.collectionId,
                notSyncedNftLocal.identifier
            )
            val syncedNftLocal = nftDao.getNft(nftIdentifier)

            val chain = chainRegistry.getChain(syncedNftLocal.chainId)
            val metaAccount = accountRepository.getMetaAccount(syncedNftLocal.metaId)

            val collection = singularV2Api.getCollection(syncedNftLocal.collectionId).first()
            val collectionMetadata = collection.metadata?.let {
                singularV2Api.getIpfsMetadata(it.adoptFileStorageLinkToHttps())
            }

            NftDetails(
                identifier = syncedNftLocal.identifier,
                chain = chain,
                owner = metaAccount.accountIdIn(chain)!!,
                creator = chain.accountIdOf(collection.issuer),
                media = syncedNftLocal.media,
                name = mapNftNameForUi(syncedNftLocal.name, syncedNftLocal.instanceId),
                description = syncedNftLocal.label,
                issuance = nftIssuance(syncedNftLocal),
                price = nftPrice(syncedNftLocal),
                collection = NftDetails.Collection(
                    id = syncedNftLocal.collectionId,
                    name = collectionMetadata?.name,
                    media = collectionMetadata?.image?.adoptFileStorageLinkToHttps()
                ),
                type = mapNftLocalToNftType(syncedNftLocal),
                tags = if (syncedNftLocal.tags == null) {
                    emptyList()
                } else {
                    gson.fromJson(syncedNftLocal.tags, List::class.java) as List<String>
                },
                attributes = mapJsonToAttributes(gson, syncedNftLocal.attributes)
            )
        }
    }

    private fun localIdentifier(chainId: ChainId, remoteId: String): String {
        return "$chainId-$remoteId"
    }
}
