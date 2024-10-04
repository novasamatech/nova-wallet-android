package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards.model

import io.novafoundation.nova.feature_governance_api.data.model.TinderGovBasketItem
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId

data class ReferendaWithBasket(
    val referenda: List<CardWithDetails>,
    val basket: Map<ReferendumId, TinderGovBasketItem>
)
