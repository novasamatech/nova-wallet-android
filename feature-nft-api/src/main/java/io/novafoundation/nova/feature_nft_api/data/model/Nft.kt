package io.novafoundation.nova.feature_nft_api.data.model

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import java.math.BigInteger

class Nft(
    val chain: Chain,
    val owner: AccountId,
    val metadata: Metadata?,
    val type: Type,
) {

    sealed class Metadata {

        class Loaded(
            val name: String,
            val label: String?,
            val media: String?,
            val price: BigInteger?,
        ) : Metadata()

        class Loadable(val rawPointer: ByteArray) : Metadata()
    }

    sealed class Type {

        class Uniques(val instanceId: BigInteger, val collectionId: BigInteger) : Type()

        class Rmrk1(val instanceId: String, val collectionId: String) : Type()

        class Rmrk2(val collectionId: String) : Type()
    }
}
