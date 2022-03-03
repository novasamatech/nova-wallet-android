package io.novafoundation.nova.feature_nft_impl.data.source.providers.rmrkV2

import io.novafoundation.nova.core_db.dao.NftDao
import io.novafoundation.nova.core_db.model.NftLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_api.data.model.NftDetails
import io.novafoundation.nova.feature_nft_impl.data.network.distributed.FileStorageAdapter.adoptFileStorageLinkToHttps
import io.novafoundation.nova.feature_nft_impl.data.source.NftProvider
import io.novafoundation.nova.feature_nft_impl.data.source.providers.rmrkV2.network.RmrkV2Api
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class RmrkV2NftProvider(
    private val api: RmrkV2Api,
    private val nftDao: NftDao
) : NftProvider {

    override suspend fun initialNftsSync(chain: Chain, metaAccount: MetaAccount, forceOverwrite: Boolean) {
        val address = metaAccount.addressIn(chain)!!
        val nfts = api.getBirds(address) + api.getItems(address)

        val toSave = nfts.map {
            NftLocal(
                identifier = identifier(chain.id, it.id),
                metaId = metaAccount.id,
                chainId = chain.id,
                collectionId = it.collectionId,
                instanceId = null,
                metadata = it.metadata.encodeToByteArray(),
                name = it.name,
                label = it.description,
                media = it.image,
                price = it.price,
                type = NftLocal.Type.RMRK2,
                issuanceTotal = null,
                issuanceMyEdition = it.edition,
                wholeDetailsLoaded = it.image != null // null in case of items, will require metadata fetch from ipfs on full sync
            )
        }

        nftDao.insertNftsDiff(NftLocal.Type.RMRK2, metaAccount.id, toSave, forceOverwrite)
    }

    override suspend fun nftFullSync(nft: Nft) {
        val metadataLink = nft.metadataRaw!!.decodeToString().adoptFileStorageLinkToHttps()
        val metadata = api.getIpfsMetadata(metadataLink)

        nftDao.updateNft(nft.identifier) { local ->
            local.copy(
                media = metadata.image.adoptFileStorageLinkToHttps(),
                wholeDetailsLoaded = true
            )
        }
    }

    override fun nftDetailsFlow(nftIdentifier: String): Flow<NftDetails> {
        return emptyFlow()
    }

    private fun identifier(chainId: ChainId, nftId: String): String {
        return "$chainId-$nftId"
    }
}
