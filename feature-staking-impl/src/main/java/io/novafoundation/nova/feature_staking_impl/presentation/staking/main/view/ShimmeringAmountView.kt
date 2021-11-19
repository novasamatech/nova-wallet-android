package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setShimmerVisible
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import kotlinx.android.synthetic.main.view_network_info_cell.view.networkInfoCellPrimaryShimmer
import kotlinx.android.synthetic.main.view_network_info_cell.view.networkInfoCellPrimaryValue
import kotlinx.android.synthetic.main.view_network_info_cell.view.networkInfoCellSecondaryShimmer
import kotlinx.android.synthetic.main.view_network_info_cell.view.networkInfoCellSecondaryValue
import kotlinx.android.synthetic.main.view_network_info_cell.view.networkInfoCellTitle

class ShimmeringAmountView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    private var usesSecondaryValue: Boolean = true

    init {
        View.inflate(context, R.layout.view_network_info_cell, this)

        orientation = VERTICAL

        attrs?.let { applyAttributes(it) }
    }

    private fun applyAttributes(attributeSet: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.ShimmeringAmountView)

        val title = typedArray.getString(R.styleable.ShimmeringAmountView_title)
        title?.let { setTitle(title) }

        usesSecondaryValue = typedArray.getBoolean(R.styleable.ShimmeringAmountView_useSecondaryValue, true)
        initSecondaryValue()

        typedArray.recycle()
    }

    private fun initSecondaryValue() {
        networkInfoCellSecondaryShimmer.setShimmerVisible(usesSecondaryValue)
    }

    fun setTitle(title: String) {
        networkInfoCellTitle.text = title
    }

    fun showLoading() {
        if (usesSecondaryValue) {
            networkInfoCellSecondaryShimmer.setShimmerVisible(true)
        }
        networkInfoCellSecondaryValue.makeGone()

        networkInfoCellPrimaryShimmer.makeVisible()
        networkInfoCellPrimaryValue.makeGone()
    }

    fun showValue(
        primary: String,
        secondary: String? = null,
    ) {
        if (secondary != null && !usesSecondaryValue) {
            throw IllegalArgumentException("Cannot set secondary value when it is not used")
        }

        networkInfoCellSecondaryShimmer.setShimmerVisible(false)
        networkInfoCellPrimaryShimmer.setShimmerVisible(false)

        secondary?.let {
            networkInfoCellSecondaryValue.makeVisible()
            networkInfoCellSecondaryValue.text = it
        }

        networkInfoCellPrimaryValue.makeVisible()
        networkInfoCellPrimaryValue.text = primary
    }
}


fun ShimmeringAmountView.showValue(amountModel: AmountModel) = showValue(amountModel.token, amountModel.fiat)
