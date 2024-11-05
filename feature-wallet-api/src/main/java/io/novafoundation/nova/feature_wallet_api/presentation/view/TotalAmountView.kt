package io.novafoundation.nova.feature_wallet_api.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.databinding.ViewTotalAmountBinding
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

class TotalAmountView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle), WithContextExtensions by WithContextExtensions(context) {

    private val binder = ViewTotalAmountBinding.inflate(inflater(), this)

    init {
        orientation = VERTICAL

        attrs?.let(::applyAttributes)
    }

    fun setAmount(amountModel: AmountModel?) {
        setAmount(amountModel?.token, amountModel?.fiat)
    }

    fun setAmount(token: String?, fiat: String?) {
        binder.totalAmountToken.text = token
        binder.totalAmountFiat.text = fiat
    }

    private fun applyAttributes(attributeSet: AttributeSet) = context.useAttributes(attributeSet, R.styleable.TotalAmountView) {
        val title = it.getString(R.styleable.TotalAmountView_title)
        binder.totalAmountTitle.text = title
    }
}
