package io.novafoundation.nova.feature_wallet_api.presentation.view.amount

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import io.novafoundation.nova.common.presentation.LoadingView
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.feature_wallet_api.databinding.ViewPrimaryAmountBinding
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

class PrimaryAmountView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle), LoadingView<AmountModel> {

    private val binder = ViewPrimaryAmountBinding.inflate(inflater(), this)

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
    }

    fun setAmount(amountModel: AmountModel) {
        binder.primaryAmountToken.makeVisible()
        binder.primaryAmountFiat.makeVisible()
        binder.primaryAmountProgress.makeGone()

        binder.primaryAmountToken.text = amountModel.token
        binder.primaryAmountFiat.setTextOrHide(amountModel.fiat)
    }

    fun setTokenAmountTextColor(@ColorRes textColor: Int) {
        binder.primaryAmountToken.setTextColorRes(textColor)
    }

    override fun showLoading() {
        binder.primaryAmountToken.makeGone()
        binder.primaryAmountFiat.makeGone()
        binder.primaryAmountProgress.makeVisible()
    }

    override fun showData(data: AmountModel) = setAmount(data)
}

fun PrimaryAmountView.setAmountOrHide(model: AmountModel?) = letOrHide(model) { nonNullModel ->
    setAmount(nonNullModel)
}
