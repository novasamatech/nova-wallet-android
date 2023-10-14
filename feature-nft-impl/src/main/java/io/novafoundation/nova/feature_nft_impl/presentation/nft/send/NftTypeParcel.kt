package io.novafoundation.nova.feature_nft_impl.presentation.nft.send

import android.os.Parcelable
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import kotlinx.android.parcel.Parcelize
import java.math.BigInteger

sealed class NftTypeParcel(val key: Key) : Parcelable {

    @Parcelize
    enum class Key : Parcelable {
        UNIQUES, RMRKV1, RMRKV2, NFTS
    }

    @Parcelize
    class Uniques(val instanceId: BigInteger, val collectionId: BigInteger) : NftTypeParcel(Key.UNIQUES)

    @Parcelize
    class Rmrk1(val instanceId: String, val collectionId: String) : NftTypeParcel(Key.RMRKV1)

    @Parcelize
    class Rmrk2(val collectionId: String) : NftTypeParcel(Key.RMRKV2)

    @Parcelize
    class Nfts(val instanceId: BigInteger, val collectionId: BigInteger) : NftTypeParcel(Key.NFTS)
}

fun Nft.Type.mapToParcel(): NftTypeParcel {
    return when (this) {
        is Nft.Type.Rmrk1 -> NftTypeParcel.Rmrk1(instanceId, collectionId)
        is Nft.Type.Rmrk2 -> NftTypeParcel.Rmrk2(collectionId)
        is Nft.Type.Uniques -> NftTypeParcel.Uniques(instanceId, collectionId)
        is Nft.Type.Nfts -> NftTypeParcel.Nfts(instanceId, collectionId)
    }
}

fun NftTypeParcel.mapToDomain(): Nft.Type {
    return when (this) {
        is NftTypeParcel.Rmrk1 -> Nft.Type.Rmrk1(instanceId, collectionId)
        is NftTypeParcel.Rmrk2 -> Nft.Type.Rmrk2(collectionId)
        is NftTypeParcel.Uniques -> Nft.Type.Uniques(instanceId, collectionId)
        is NftTypeParcel.Nfts -> Nft.Type.Nfts(instanceId, collectionId)
    }
}
