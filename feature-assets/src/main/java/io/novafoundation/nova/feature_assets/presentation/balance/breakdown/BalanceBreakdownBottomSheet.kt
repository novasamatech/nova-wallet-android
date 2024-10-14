package io.novafoundation.nova.feature_assets.presentation.balance.breakdown

import android.content.Context
import android.view.LayoutInflater
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.databinding.FragmentBalanceBreakdownBinding
import io.novafoundation.nova.feature_assets.presentation.balance.breakdown.model.TotalBalanceBreakdownModel

class BalanceBreakdownBottomSheet(context: Context) : BaseBottomSheet<FragmentBalanceBreakdownBinding>(context) {

    override val binder: FragmentBalanceBreakdownBinding = FragmentBalanceBreakdownBinding.inflate(LayoutInflater.from(context))

    private var totalBreakdown: TotalBalanceBreakdownModel? = null

    private val adapter = BalanceBreakdownAdapter()

    init {
        setContentView(R.layout.fragment_balance_breakdown)
        binder.balanceBreakdownList.adapter = adapter
    }

    fun setBalanceBreakdown(totalBreakdown: TotalBalanceBreakdownModel) {
        this.totalBreakdown = totalBreakdown
        binder.balanceBreakdownTotal.text = totalBreakdown.totalFiat
        adapter.submitList(totalBreakdown.breakdown)
    }
}
