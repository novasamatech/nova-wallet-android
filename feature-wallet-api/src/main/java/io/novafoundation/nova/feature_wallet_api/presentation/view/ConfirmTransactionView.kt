package io.novafoundation.nova.feature_wallet_api.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.setBackgroundColorRes
import io.novafoundation.nova.common.view.PrimaryButton
import io.novafoundation.nova.feature_wallet_api.R
import kotlinx.android.synthetic.main.view_confirm_transaction.view.confirmTransactionAction
import kotlinx.android.synthetic.main.view_confirm_transaction.view.confirmTransactionFee

class ConfirmTransactionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    val fee: FeeView
        get() = confirmTransactionFee

    val submit: PrimaryButton
        get() = confirmTransactionAction

    init {
        View.inflate(context, R.layout.view_confirm_transaction, this)

        orientation = VERTICAL

        setBackgroundColorRes(R.color.black4)
    }
}
