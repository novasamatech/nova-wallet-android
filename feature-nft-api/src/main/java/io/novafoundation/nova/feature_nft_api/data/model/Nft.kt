package io.novafoundation.nova.feature_nft_api.data.model

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
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
            val price: Price?,
            val issuance: Issuance,
            val name: String?,
            val label: String?,
            val media: String?,
        ) : Details()



        object Loadable : Details()
    }

    sealed class Price {

        class NonFungible(val nftPrice: BigInteger): Price()

        class Fungible(val units: BigInteger, val totalPrice: Balance): Price()
    }

    sealed class Issuance {

        object Unlimited : Issuance()

        class Limited(val max: Int, val edition: Int) : Issuance()

        class Fungible(val myAmount: BigInteger, val totalSupply: BigInteger): Issuance()
    }

    sealed class Type(val key: Key) {

        enum class Key {
            UNIQUES, RMRKV1, RMRKV2, PDC20
        }

        object Uniques : Type(Key.UNIQUES)

        object Rmrk1 : Type(Key.RMRKV1)

        object Rmrk2 : Type(Key.RMRKV2)

        object Pdc20 : Type(Key.RMRKV2)
    }
}

val Nft.isFullySynced
    get() = details is Nft.Details.Loaded
