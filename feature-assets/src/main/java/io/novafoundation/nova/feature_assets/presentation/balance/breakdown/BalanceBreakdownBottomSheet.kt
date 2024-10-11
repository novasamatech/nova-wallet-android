package io.novafoundation.nova.feature_assets.presentation.balance.breakdown

import android.content.Context
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.presentation.balance.breakdown.model.TotalBalanceBreakdownModel

class BalanceBreakdownBottomSheet(context: Context) : BaseBottomSheet(context) {

    private var totalBreakdown: TotalBalanceBreakdownModel? = null

    private val adapter = BalanceBreakdownAdapter()

    init {
        setContentView(R.layout.fragment_balance_breakdown)
        balanceBreakdownList.adapter = adapter
    }

    fun setBalanceBreakdown(totalBreakdown: TotalBalanceBreakdownModel) {
        this.totalBreakdown = totalBreakdown
        balanceBreakdownTotal.text = totalBreakdown.totalFiat
        adapter.submitList(totalBreakdown.breakdown)
    }
}
