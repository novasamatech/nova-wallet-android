package io.novafoundation.nova.feature_wallet_api.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.StringRes
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.updatePadding
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.common.view.TableCellView
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.common.view.showValueOrHide
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import kotlinx.android.synthetic.main.view_balances.view.viewBalancesTitle

abstract class BalancesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    init {
        View.inflate(context, R.layout.view_balances, this)
        orientation = VERTICAL

        val commonPadding = 16.dp(context)

        updatePadding(
            top = commonPadding,
            start = commonPadding,
            end = commonPadding,
            bottom = 8.dp(context)
        )

        attrs?.let {
            applyAttributes(it)
        }

        background = context.getBlockDrawable()
    }

    private fun applyAttributes(attributes: AttributeSet) = context.useAttributes(attributes, R.styleable.BalancesView) {
        val title = it.getString(R.styleable.BalancesView_title)
        viewBalancesTitle.text = title
    }

    protected fun item(@StringRes titleRes: Int): TableCellView {
        val item = TableCellView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

            valueSecondary.setTextColorRes(R.color.text_tertiary)
            title.setTextColorRes(R.color.text_tertiary)

            setTitle(titleRes)
        }

        addView(item)

        return item
    }
}

fun TableCellView.showAmount(amountModel: AmountModel) {
    showValue(amountModel.token, amountModel.fiat)
}

fun TableCellView.showAmountOrHide(amountModel: AmountModel?) {
    showValueOrHide(amountModel?.token, amountModel?.fiat)
}
