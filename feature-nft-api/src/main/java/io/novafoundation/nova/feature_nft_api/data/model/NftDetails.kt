package io.novafoundation.nova.feature_nft_api.data.model

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class NftDetails(
    val identifier: String,
    val chain: Chain,
    val owner: AccountId,
    val creator: AccountId?,
    val media: String?,
    val name: String,
    val description: String?,
    val issuance: Nft.Issuance,
    val price: Nft.Price?,
    val collection: Collection?
) {

    class Collection(
        val id: String,
        val name: String? = null,
        val media: String? = null
    )
}
