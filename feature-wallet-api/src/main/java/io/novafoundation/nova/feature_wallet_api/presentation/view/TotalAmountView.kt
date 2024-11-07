package io.novafoundation.nova.feature_wallet_api.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import kotlinx.android.synthetic.main.view_total_amount.view.totalAmountFiat
import kotlinx.android.synthetic.main.view_total_amount.view.totalAmountTitle
import kotlinx.android.synthetic.main.view_total_amount.view.totalAmountToken

class TotalAmountView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle), WithContextExtensions by WithContextExtensions(context) {
    init {
        orientation = VERTICAL

        View.inflate(context, R.layout.view_total_amount, this)

        attrs?.let(::applyAttributes)
    }

    fun setAmount(amountModel: AmountModel?) {
        setAmount(amountModel?.token, amountModel?.fiat)
    }

    fun setAmount(token: CharSequence?, fiat: CharSequence?) {
        totalAmountToken.text = token
        totalAmountFiat.text = fiat
    }

    private fun applyAttributes(attributeSet: AttributeSet) = context.useAttributes(attributeSet, R.styleable.TotalAmountView) {
        val title = it.getString(R.styleable.TotalAmountView_title)
        totalAmountTitle.text = title
    }
}
