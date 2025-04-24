package io.novafoundation.nova.feature_nft_impl.data.source.providers.kodadot

import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.scopeAsync
import io.novafoundation.nova.core_db.dao.NftDao
import io.novafoundation.nova.core_db.model.NftLocal
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_api.data.model.NftDetails
import io.novafoundation.nova.feature_nft_impl.data.mappers.nftIssuance
import io.novafoundation.nova.feature_nft_impl.data.mappers.nftPrice
import io.novafoundation.nova.feature_nft_impl.data.network.distributed.FileStorageAdapter.adoptFileStorageLinkToHttps
import io.novafoundation.nova.feature_nft_impl.data.source.NftProvider
import io.novafoundation.nova.feature_nft_impl.data.source.providers.kodadot.network.KodadotApi
import io.novafoundation.nova.feature_nft_impl.data.source.providers.kodadot.network.request.KodadotCollectionRequest
import io.novafoundation.nova.feature_nft_impl.data.source.providers.kodadot.network.request.KodadotMetadataRequest
import io.novafoundation.nova.feature_nft_impl.data.source.providers.kodadot.network.response.KodadotNftRemote
import io.novafoundation.nova.feature_nft_impl.data.source.providers.kodadot.network.request.KodadotNftsRequest
import io.novafoundation.nova.feature_nft_impl.data.source.providers.kodadot.network.response.KodadotCollectionRemote
import io.novafoundation.nova.feature_nft_impl.data.source.providers.kodadot.network.response.KodadotMetadataRemote
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow

private const val NO_COLLECTION_LOCAL_ID = "no_collection_local_id"

class KodadotProvider(
    private val api: KodadotApi,
    private val nftDao: NftDao,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
) : NftProvider {

    override val requireFullChainSync: Boolean = false

    override suspend fun initialNftsSync(chain: Chain, metaAccount: MetaAccount, forceOverwrite: Boolean) {
        val address = metaAccount.addressIn(chain) ?: return

        val apiUrl = getApiUrl(chain) ?: return
        val request = KodadotNftsRequest(address)
        val nfts = api.getNfts(apiUrl, request)

        val toSave = nfts.data.nftEntities.map { nftRemote ->
            NftLocal(
                identifier = nftIdentifier(chain, nftRemote),
                metaId = metaAccount.id,
                chainId = chain.id,
                collectionId = nftRemote.collection?.id ?: NO_COLLECTION_LOCAL_ID,
                instanceId = nftRemote.id,
                metadata = nftRemote.metadata?.encodeToByteArray(),
                type = NftLocal.Type.KODADOT,
                wholeDetailsLoaded = true,
                name = nftRemote.name,
                label = nftRemote.sn,
                media = nftRemote.image?.adoptFileStorageLinkToHttps(),
                issuanceType = NftLocal.IssuanceType.LIMITED,
                issuanceTotal = nftRemote.collection?.max?.let { BigInteger(it) },
                issuanceMyAmount = null,
                price = nftRemote.price?.let { BigInteger(it) },
                pricedUnits = null
            )
        }

        nftDao.insertNftsDiff(NftLocal.Type.KODADOT, chain.id, metaAccount.id, toSave, forceOverwrite)
    }

    override suspend fun nftFullSync(nft: Nft) {
        // do nothing
    }

    override fun nftDetailsFlow(nftIdentifier: String): Flow<NftDetails> {
        return flowOf {
            val nftLocal = nftDao.getNft(nftIdentifier)
            require(nftLocal.wholeDetailsLoaded) {
                "Cannot load details of non fully-synced NFT"
            }

            val chain = chainRegistry.getChain(nftLocal.chainId)
            val metaAccount = accountRepository.getMetaAccount(nftLocal.metaId)

            val metadataDeferred = scopeAsync { fetchMetadata(nftLocal, chain) }
            val collectionDeferred = scopeAsync { fetchCollection(nftLocal, chain) }

            val metadata = metadataDeferred.await()
            val collection = collectionDeferred.await()

            NftDetails(
                identifier = nftLocal.identifier,
                chain = chain,
                owner = metaAccount.requireAccountIdIn(chain),
                creator = collection?.issuer?.let { chain.accountIdOf(it) },
                media = metadata?.image?.adoptFileStorageLinkToHttps() ?: nftLocal.media,
                name = metadata?.name ?: nftLocal.name ?: nftLocal.instanceId!!,
                description = metadata?.description,
                issuance = nftIssuance(nftLocal),
                price = nftPrice(nftLocal),
                collection = collection?.let {
                    NftDetails.Collection(
                        id = nftLocal.collectionId,
                        name = it.name,
                        media = it.image
                    )
                }
            )
        }
    }

    private suspend fun fetchMetadata(nftLocal: NftLocal, chain: Chain): KodadotMetadataRemote? {
        val metadataId = nftLocal.metadata?.decodeToString() ?: return null
        val apiUrl = getApiUrl(chain) ?: return null
        val request = KodadotMetadataRequest(metadataId)
        return api.getMetadata(apiUrl, request)
            .data
            .metadataEntityById
    }

    private suspend fun fetchCollection(nftLocal: NftLocal, chain: Chain): KodadotCollectionRemote? {
        val collectionId = nftLocal.collectionId
        if (collectionId == NO_COLLECTION_LOCAL_ID) {
            return null
        }

        val apiUrl = getApiUrl(chain) ?: return null
        val request = KodadotCollectionRequest(collectionId)
        return api.getCollection(apiUrl, request)
            .data
            .collectionEntityById
    }

    private fun nftIdentifier(chain: Chain, nft: KodadotNftRemote): String {
        return "kodadot-${chain.id}-${nft.id}"
    }

    private fun getApiUrl(chain: Chain): String? {
        return when (chain.id) {
            ChainGeneses.POLKADOT_ASSET_HUB -> KodadotApi.POLKADOT_ASSET_HUB_URL
            ChainGeneses.KUSAMA_ASSET_HUB -> KodadotApi.KUSAMA_ASSET_HUB_URL
            else -> null
        }
    }
}
