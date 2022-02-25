package io.novafoundation.nova.feature_nft_impl.data.source.providers.rmrkV1

import io.novafoundation.nova.core_db.dao.NftDao
import io.novafoundation.nova.core_db.model.NftLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.feature_nft_impl.data.source.NftProvider
import io.novafoundation.nova.feature_nft_impl.data.source.providers.rmrkV1.network.RmrkV1Api
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class RmrkV1NftProvider(
    private val api: RmrkV1Api,
    private val nftDao: NftDao
) : NftProvider {

    override suspend fun initialNftsSync(chain: Chain, metaAccount: MetaAccount) {
        val address = metaAccount.addressIn(chain)!!
        val nfts = api.getNfts(address)

        val toSave = nfts.map {
            NftLocal(
                identifier = identifier(chain.id, it.id),
                metaId = metaAccount.id,
                chainId = chain.id,
                collectionId = it.collectionId,
                instanceId = it.instance,
                metadata = it.metadata.encodeToByteArray(),
                name = it.name,
                label = null,
                media = it.metadataImage,
                price = it.price,
                type = NftLocal.Type.RMRK1,
                issuanceMyEdition = it.edition,

                // to load at full sync
                issuanceTotal = null,

                wholeDetailsLoaded = false
            )
        }

        nftDao.insertNftsDiff(NftLocal.Type.RMRK1, metaAccount.id, toSave)
    }

    override suspend fun nftFullSync(nft: Nft) {
        // TODO
    }

    private fun identifier(chainId: ChainId, id: String): String {
        return "$chainId-$id"
    }
}
