package io.novafoundation.nova.feature_pay_impl.presentation.shop.main

sealed interface PurchasedCardsState {

    data object Empty : PurchasedCardsState

    data class Content(val quantity: Int) : PurchasedCardsState
}
