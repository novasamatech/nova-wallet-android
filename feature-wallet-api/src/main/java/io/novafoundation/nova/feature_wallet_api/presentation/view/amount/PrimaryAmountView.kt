package io.novafoundation.nova.feature_wallet_api.presentation.view.amount

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import io.novafoundation.nova.common.presentation.LoadingView
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import kotlinx.android.synthetic.main.view_primary_amount.view.primaryAmountFiat
import kotlinx.android.synthetic.main.view_primary_amount.view.primaryAmountProgress
import kotlinx.android.synthetic.main.view_primary_amount.view.primaryAmountToken

class PrimaryAmountView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle), LoadingView<AmountModel> {

    init {
        View.inflate(context, R.layout.view_primary_amount, this)
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
    }

    fun setAmount(amountModel: AmountModel) {
        primaryAmountToken.text = amountModel.token
        primaryAmountFiat.text = amountModel.fiat

        primaryAmountToken.makeVisible()
        primaryAmountFiat.makeVisible()
        primaryAmountProgress.makeGone()
    }

    override fun showLoading() {
        primaryAmountToken.makeGone()
        primaryAmountFiat.makeGone()
        primaryAmountProgress.makeVisible()
    }

    override fun showData(data: AmountModel) = setAmount(data)
}
