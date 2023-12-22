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
    NftLocal.Type.PDC20 -> Nft.Type.Key.PDC20
}

fun nftIssuance(
    typeLocal: NftLocal.IssuanceType,
    issuanceTotal: BigInteger?,
    issuanceMyEdition: String?,
    issuanceMyAmount: BigInteger?
): Nft.Issuance {
    return when(typeLocal) {
        NftLocal.IssuanceType.UNLIMITED -> Nft.Issuance.Unlimited

        NftLocal.IssuanceType.LIMITED -> {
            val myEditionInt = issuanceMyEdition?.toIntOrNull()

            if (issuanceTotal != null && myEditionInt != null) {
                Nft.Issuance.Limited(max = issuanceTotal.toInt(), edition = myEditionInt)
            } else {
                Nft.Issuance.Unlimited
            }
        }
        NftLocal.IssuanceType.FUNGIBLE -> if (issuanceTotal != null && issuanceMyAmount != null) {
            Nft.Issuance.Fungible(myAmount = issuanceMyAmount, totalSupply = issuanceTotal)
        } else {
            Nft.Issuance.Unlimited
        }
    }
}

fun nftIssuance(nftLocal: NftLocal): Nft.Issuance {
    require(nftLocal.wholeDetailsLoaded)

    return nftIssuance(nftLocal.issuanceType, nftLocal.issuanceTotal, nftLocal.issuanceMyEdition, nftLocal.issuanceMyAmount)
}

fun nftPrice(nftLocal: NftLocal): Nft.Price? {
    val price = nftLocal.price
    if (price == null || price == BigInteger.ZERO) return null

    return when (val units = nftLocal.pricedUnits) {
        null -> Nft.Price.NonFungible(price)
        else -> Nft.Price.Fungible(units = units, totalPrice = price)
    }
}

fun mapNftLocalToNft(
    chainsById: Map<ChainId, Chain>,
    metaAccount: MetaAccount,
    nftLocal: NftLocal
): Nft? {
    val chain = chainsById[nftLocal.chainId] ?: return null

    val type = when (nftLocal.type) {
        NftLocal.Type.UNIQUES -> Nft.Type.Uniques
        NftLocal.Type.RMRK1 -> Nft.Type.Rmrk1
        NftLocal.Type.RMRK2 -> Nft.Type.Rmrk2
        NftLocal.Type.PDC20 -> Nft.Type.Pdc20
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
