package io.novafoundation.nova.feature_assets.presentation.balance.breakdown

import android.content.Context
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.presentation.balance.breakdown.model.TotalBreakdownModel
import kotlinx.android.synthetic.main.fragment_balance_breakdown.balanceBreakdownList
import kotlinx.android.synthetic.main.fragment_balance_breakdown.balanceBreakdownTotal

class BalanceBreakdownBottomSheet(context: Context) : BaseBottomSheet(context) {

    private var totalBreakdown: TotalBreakdownModel? = null

    private val adapter = BalanceBreakdownAdapter()

    init {
        setContentView(R.layout.fragment_balance_breakdown)
        balanceBreakdownList.adapter = adapter
    }

    fun setBalanceBreakdown(totalBreakdown: TotalBreakdownModel) {
        this.totalBreakdown = totalBreakdown
        balanceBreakdownTotal.text = totalBreakdown.totalFiat
        adapter.submitList(totalBreakdown.breakdown)
    }
}
