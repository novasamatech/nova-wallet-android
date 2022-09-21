package io.novafoundation.nova.feature_assets.presentation.balance.breakdown.model

import androidx.annotation.DrawableRes
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

interface BalanceBreakdownItem {
    val name: String
}

data class BalanceBreakdownAmount(
    override val name: String,
    val amount: AmountModel
) : BalanceBreakdownItem

data class BalanceBreakdownTotal(
    override val name: String,
    val fiatAmount: String,
    @DrawableRes val iconRes: Int,
    val percentage: String
) : BalanceBreakdownItem
