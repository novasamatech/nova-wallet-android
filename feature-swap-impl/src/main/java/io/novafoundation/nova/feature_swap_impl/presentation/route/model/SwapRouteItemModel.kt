package io.novafoundation.nova.feature_swap_impl.presentation.route.model

import io.novafoundation.nova.feature_swap_impl.presentation.route.view.TokenAmountModel

sealed class SwapRouteItemModel {

    abstract val id: Int

    data class Transfer(
        override val id: Int,
        val amount: TokenAmountModel,
        val fee: String,
        val originChainName: String,
        val destinationChainName: String,
    ): SwapRouteItemModel()

    data class Swap(
        override val id: Int,
        val amountFrom: TokenAmountModel,
        val amountTo: TokenAmountModel,
        val fee: String,
        val chain: String
    ): SwapRouteItemModel()
}
