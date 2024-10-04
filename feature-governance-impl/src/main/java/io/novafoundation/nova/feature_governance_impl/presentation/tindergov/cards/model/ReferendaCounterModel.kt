package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards.model

class ReferendaCounterModel(
    val itemsInBasket: Int,
    val referendaSize: Int
) {

    val remainingReferendaToVote: Int
        get() = referendaSize - itemsInBasket

    fun hasReferendaToVote(): Boolean {
        return referendaSize > itemsInBasket
    }
}
