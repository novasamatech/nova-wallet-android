package io.novafoundation.nova.feature_assets.presentation.balance.breakdown.model

import androidx.annotation.DrawableRes

interface BalanceBreakdownItem {
    val name: String
    val amount: String
}

data class BalanceBreakdownAmount(
    override val name: String,
    override val amount: String
) : BalanceBreakdownItem

data class BalanceBreakdownTotal(
    override val name: String,
    override val amount: String,
    @DrawableRes val iconRes: Int,
    val percentage: String
) : BalanceBreakdownItem
