package io.novafoundation.nova.feature_wallet_api.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setBackgroundColorRes
import io.novafoundation.nova.common.view.PrimaryButton
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.databinding.ViewConfirmTransactionBinding

class ConfirmTransactionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    private val binder = ViewConfirmTransactionBinding.inflate(inflater(), this)

    val fee: FeeView
        get() = binder.confirmTransactionFee

    val submit: PrimaryButton
        get() = binder.confirmTransactionAction

    init {
        View.inflate(context, R.layout.view_confirm_transaction, this)

        orientation = VERTICAL

        setBackgroundColorRes(R.color.secondary_screen_background)
    }
}
