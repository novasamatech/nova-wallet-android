package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.basket.adpter

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId

data class TinderGovBasketRvItem(
    val id: ReferendumId,
    val idStr: String,
    val title: String,
    val subtitle: CharSequence
)
