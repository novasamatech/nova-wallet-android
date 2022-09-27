package io.novafoundation.nova.feature_wallet_api.presentation.view.amount

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.presentation.model.ChooseAmountModel
import kotlinx.android.synthetic.main.view_choose_amount.view.chooseAmountBalance
import kotlinx.android.synthetic.main.view_choose_amount.view.chooseAmountBalanceLabel
import kotlinx.android.synthetic.main.view_choose_amount.view.chooseAmountInput
import kotlinx.android.synthetic.main.view_choose_amount.view.chooseAmountTitle

class ChooseAmountView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle) {

    val amountInput: EditText
        get() = chooseAmountInput.amountInput

    init {
        View.inflate(context, R.layout.view_choose_amount, this)

        attrs?.let(::applyAttrs)
    }

    fun setBalanceLabel(label: String?) {
        chooseAmountBalanceLabel.setTextOrHide(label)
    }

    fun setBalance(balance: String?) {
        chooseAmountBalance.setTextOrHide(balance)
    }

    fun loadAssetImage(imageUrl: String) {
        chooseAmountInput.loadAssetImage(imageUrl)
    }

    fun setTitle(title: String?) {
        chooseAmountTitle.setTextOrHide(title)
    }

    fun setAssetName(name: String) {
        chooseAmountInput.setAssetName(name)
    }

    fun setFiatAmount(priceAmount: String?) {
        chooseAmountInput.setFiatAmount(priceAmount)
    }

    private fun applyAttrs(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.ChooseAmountView) {
        val title = it.getString(R.styleable.ChooseAmountView_title) ?: context.getString(R.string.common_amount)
        setTitle(title)
    }
}

fun ChooseAmountView.setChooseAmountModel(chooseAmountModel: ChooseAmountModel) {
    setBalanceLabel(chooseAmountModel.balanceLabel)

    setBalance(chooseAmountModel.balance)

    chooseAmountInput.setChooseAmountInputModel(chooseAmountModel.input)
}
