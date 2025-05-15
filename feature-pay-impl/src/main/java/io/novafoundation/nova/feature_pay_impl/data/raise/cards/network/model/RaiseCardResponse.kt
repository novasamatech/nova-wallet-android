package io.novafoundation.nova.feature_pay_impl.data.raise.cards.network.model

import io.novafoundation.nova.feature_pay_impl.data.raise.brands.network.model.RaiseBrandResponse

class RaiseCardsResponse(
    val data: List<RaiseCardData>,
    val included: List<RaiseBrandResponse>?
)

class RaiseCardData(
    val attributes: RaiseCardRemote,
    val id: String
)

