package io.novafoundation.nova.feature_wallet_api.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.StringRes
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.TableCellView
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.common.view.showLoadingState
import io.novafoundation.nova.common.view.showValueOrHide
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import kotlinx.android.synthetic.main.view_balances.view.viewBalanceExpandableContainer
import kotlinx.android.synthetic.main.view_balances.view.viewBalanceExpandableView
import kotlinx.android.synthetic.main.view_balances.view.viewBalanceFiat
import kotlinx.android.synthetic.main.view_balances.view.viewBalanceToken

abstract class BalancesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    protected val expandableView
        get() = viewBalanceExpandableView

    init {
        View.inflate(context, R.layout.view_balances, this)
        orientation = VERTICAL

        background = context.getBlockDrawable()
    }

    fun setTotalBalance(token: CharSequence, fiat: CharSequence?) {
        viewBalanceToken.text = token
        viewBalanceFiat.setTextOrHide(fiat)
    }

    protected fun item(@StringRes titleRes: Int): TableCellView {
        val item = TableCellView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

            valueSecondary.setTextColorRes(R.color.text_secondary)
            title.setTextColorRes(R.color.text_secondary)
            setPadding(16.dp, 0, 16.dp, 0)

            isClickable = true // To not propagate parent state to children. isDuplicateParentState not working in this case
            setTitle(titleRes)
        }

        viewBalanceExpandableContainer.addView(item)

        return item
    }
}

fun BalancesView.setTotalAmount(amountModel: AmountModel) {
    setTotalBalance(amountModel.token, amountModel.fiat)
}

fun TableCellView.showAmount(amountModel: AmountModel) {
    showValue(amountModel.token, amountModel.fiat)
}

fun TableCellView.showAmountOrHide(amountModel: AmountModel?) {
    showValueOrHide(amountModel?.token, amountModel?.fiat)
}

fun TableCellView.showLoadingAmount(model: ExtendedLoadingState<AmountModel?>) = showLoadingState(model, ::showAmountOrHide)
