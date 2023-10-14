package io.novafoundation.nova.feature_nft_impl.presentation.nft.details

import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

class NftDetailsModel(
    val media: String?,
    val name: String,
    val issuance: String,
    val description: String?,
    val price: AmountModel?,
    val collection: Collection?,
    val owner: AddressModel,
    val creator: AddressModel?,
    val network: ChainUi,
    val isSupportedForSend: Boolean,
    val tags: List<String>,
    val attributes: List<Attribute>
) {

    class Collection(
        val name: String,
        val media: String?
    )

    class Attribute(
        val label: String,
        val value: String
    )
}
