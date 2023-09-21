package io.novafoundation.nova.feature_nft_api.data.model

import android.os.Parcelable
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.android.parcel.Parcelize
import java.math.BigInteger

class Nft(
    val identifier: String,
    val instanceId: String?,
    val collectionId: String,
    val chain: Chain,
    val owner: AccountId,
    val metadataRaw: ByteArray?,
    val details: Details,
    val type: Type,
    val wholeDetailsLoaded: Boolean
) {

    sealed class Details {

        class Loaded(
            val price: BigInteger?,
            val collectionName: String?,
            val name: String?,
            val label: String?,
            val media: String?,
            val collectionMedia: String?,
        ) : Details()

        object Loadable : Details()
    }

    sealed class Issuance {
        class Unlimited(val edition: String) : Issuance()

        class Limited(val max: Int, val edition: Int) : Issuance()
    }

    sealed class Type(val key: Key): Parcelable {

        @Parcelize
        enum class Key: Parcelable {
            UNIQUES, RMRKV1, RMRKV2
        }

        @Parcelize
        class Uniques(val instanceId: BigInteger, val collectionId: BigInteger) : Type(Key.UNIQUES)

        @Parcelize
        class Rmrk1(val instanceId: String, val collectionId: String) : Type(Key.RMRKV1)

        @Parcelize
        class Rmrk2(val collectionId: String) : Type(Key.RMRKV2)
    }
}

val Nft.isFullySynced
    get() = details is Nft.Details.Loaded
