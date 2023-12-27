package io.novafoundation.nova.feature_nft_api.data.model

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
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
) {

    sealed class Details {

        class Loaded(
            val price: BigInteger?,
            val issuance: Issuance,
            val name: String?,
            val label: String?,
            val media: String?,
        ) : Details()

        object Loadable : Details()
    }

    sealed class Issuance {
        class Unlimited(val edition: String) : Issuance()

        class Limited(val max: Int, val edition: Int) : Issuance()
    }

    sealed class Type(val key: Key) {

        enum class Key {
            UNIQUES, RMRKV1, RMRKV2
        }

        class Uniques(val instanceId: BigInteger, val collectionId: BigInteger) : Type(Key.UNIQUES)

        class Rmrk1(val instanceId: String, val collectionId: String) : Type(Key.RMRKV1)

        class Rmrk2(val collectionId: String) : Type(Key.RMRKV2)
    }
}

val Nft.isFullySynced
    get() = details is Nft.Details.Loaded
