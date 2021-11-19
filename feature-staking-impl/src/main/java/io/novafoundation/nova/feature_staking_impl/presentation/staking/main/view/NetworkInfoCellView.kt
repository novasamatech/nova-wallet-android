package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_staking_impl.R
import kotlinx.android.synthetic.main.view_network_info_cell.view.networkInfoCellPrimaryShimmer
import kotlinx.android.synthetic.main.view_network_info_cell.view.networkInfoCellPrimaryValue
import kotlinx.android.synthetic.main.view_network_info_cell.view.networkInfoCellSecondaryShimmer
import kotlinx.android.synthetic.main.view_network_info_cell.view.networkInfoCellSecondaryValue
import kotlinx.android.synthetic.main.view_network_info_cell.view.networkInfoCellTitle

class NetworkInfoCellView @JvmOverloads constructor(
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
        val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.NetworkInfoCellView)

        val title = typedArray.getString(R.styleable.NetworkInfoCellView_title)
        title?.let { setTitle(title) }

        usesSecondaryValue = typedArray.getBoolean(R.styleable.NetworkInfoCellView_useSecondaryValue, true)
        initSecondaryValue()

        typedArray.recycle()
    }

    private fun initSecondaryValue() {
        networkInfoCellSecondaryShimmer.setVisible(usesSecondaryValue)
    }

    fun setTitle(title: String) {
        networkInfoCellTitle.text = title
    }

    fun showLoading() {
        if (usesSecondaryValue) {
            networkInfoCellSecondaryShimmer.makeVisible()
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

        networkInfoCellSecondaryShimmer.makeGone()
        networkInfoCellPrimaryShimmer.makeGone()

        secondary?.let {
            networkInfoCellSecondaryValue.makeVisible()
            networkInfoCellSecondaryValue.text = it
        }

        networkInfoCellPrimaryValue.makeVisible()
        networkInfoCellPrimaryValue.text = primary
    }
}
