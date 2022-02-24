package io.novafoundation.nova.feature_nft_impl.data.mappers

import io.novafoundation.nova.core_db.model.NftLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

fun mapNftLocalToNft(
    chainsById: Map<ChainId, Chain>,
    metaAccount: MetaAccount,
    nftLocal: NftLocal
): Nft? {
    val chain = chainsById[nftLocal.chainId] ?: return null

    val type = when(nftLocal.type) {
        NftLocal.Type.UNIQUES -> Nft.Type.Uniques(
            instanceId = nftLocal.instanceId!!.toBigInteger(),
            collectionId = nftLocal.collectionId!!.toBigInteger(),
        )
        NftLocal.Type.RMRK1 -> Nft.Type.Rmrk1(
            instanceId = nftLocal.instanceId!!,
            collectionId = nftLocal.collectionId!!
        )
    }

    val metadata = when {
        nftLocal.metadata == null -> null // not present
        nftLocal.name == null -> Nft.Metadata.Loadable(nftLocal.metadata!!) // not loaded
        else -> Nft.Metadata.Loaded(
            name = nftLocal.name!!,
            label = nftLocal.label,
            media = nftLocal.media,
            price = nftLocal.price
        ) // loaded
    }


    return Nft(
        chain = chain,
        owner = metaAccount.accountIdIn(chain)!!,
        metadata = metadata,
        type = type
    )
}
