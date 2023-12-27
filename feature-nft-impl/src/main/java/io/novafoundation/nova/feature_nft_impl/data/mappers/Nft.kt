package io.novafoundation.nova.feature_nft_impl.data.mappers

import io.novafoundation.nova.core_db.model.NftLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import java.math.BigInteger

fun mapNftTypeLocalToTypeKey(
    typeLocal: NftLocal.Type
): Nft.Type.Key = when (typeLocal) {
    NftLocal.Type.UNIQUES -> Nft.Type.Key.UNIQUES
    NftLocal.Type.RMRK1 -> Nft.Type.Key.RMRKV1
    NftLocal.Type.RMRK2 -> Nft.Type.Key.RMRKV2
}

fun nftIssuance(nftLocal: NftLocal): Nft.Issuance {
    require(nftLocal.wholeDetailsLoaded)

    return if (nftLocal.issuanceTotal != null && nftLocal.issuanceTotal!! > 0) {
        Nft.Issuance.Limited(
            max = nftLocal.issuanceTotal!!,
            edition = nftLocal.issuanceMyEdition!!.toInt()
        )
    } else {
        Nft.Issuance.Unlimited(nftLocal.issuanceMyEdition!!)
    }
}

fun nftPrice(nftLocal: NftLocal): BigInteger? {
    return if (nftLocal.price == BigInteger.ZERO) {
        null
    } else {
        nftLocal.price
    }
}

fun mapNftLocalToNft(
    chainsById: Map<ChainId, Chain>,
    metaAccount: MetaAccount,
    nftLocal: NftLocal
): Nft? {
    val chain = chainsById[nftLocal.chainId] ?: return null

    val type = when (nftLocal.type) {
        NftLocal.Type.UNIQUES -> Nft.Type.Uniques(
            instanceId = nftLocal.instanceId!!.toBigInteger(),
            collectionId = nftLocal.collectionId.toBigInteger(),
        )
        NftLocal.Type.RMRK1 -> Nft.Type.Rmrk1(
            instanceId = nftLocal.instanceId!!,
            collectionId = nftLocal.collectionId
        )
        NftLocal.Type.RMRK2 -> Nft.Type.Rmrk2(
            collectionId = nftLocal.collectionId
        )
    }

    val details = if (nftLocal.wholeDetailsLoaded) {
        val issuance = nftIssuance(nftLocal)

        Nft.Details.Loaded(
            name = nftLocal.name,
            label = nftLocal.label,
            media = nftLocal.media,
            price = nftPrice(nftLocal),
            issuance = issuance,
        )
    } else {
        Nft.Details.Loadable
    }

    return Nft(
        identifier = nftLocal.identifier,
        instanceId = nftLocal.instanceId,
        collectionId = nftLocal.collectionId,
        chain = chain,
        owner = metaAccount.accountIdIn(chain)!!,
        metadataRaw = nftLocal.metadata,
        type = type,
        details = details
    )
}
