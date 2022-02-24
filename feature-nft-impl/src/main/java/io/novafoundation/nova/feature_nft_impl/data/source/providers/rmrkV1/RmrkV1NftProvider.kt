package io.novafoundation.nova.feature_nft_impl.data.source.providers.rmrkV1

import io.novafoundation.nova.core_db.dao.NftDao
import io.novafoundation.nova.core_db.model.NftLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
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
                identifier = identifier(chain.id, it.collectionId, it.instance),
                metaId = metaAccount.id,
                chainId = chain.id,
                collectionId = it.collectionId,
                instanceId = it.instance,
                metadata = it.metadata.encodeToByteArray(),
                name = it.name,
                label = null,
                media = it.metadataImage,
                price = it.price,
                type = NftLocal.Type.RMRK1
            )
        }

        nftDao.insertNfts(toSave)
    }

    private fun identifier(chainId: ChainId, collectionId: String, instanceId: String): String {
        return "$chainId-$collectionId-$instanceId"
    }
}
