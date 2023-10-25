package io.novafoundation.nova.feature_wallet_api.presentation.view.amount

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.MaxActionAvailability
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.MaxAvailableView
import kotlinx.android.synthetic.main.view_max_amount.view.viewMaxAmountAction
import kotlinx.android.synthetic.main.view_max_amount.view.viewMaxAmountValue

class MaxAmountView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle), MaxAvailableView {

    init {
        View.inflate(context, R.layout.view_max_amount, this)

        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    override fun setMaxAmountDisplay(maxAmountDisplay: String?) = letOrHide(maxAmountDisplay) { display ->
        viewMaxAmountValue.text = display
    }

    override fun setMaxActionAvailability(availability: MaxActionAvailability) {
        when (availability) {
            is MaxActionAvailability.Available -> {
                setOnClickListener(availability.onMaxClicked)
                viewMaxAmountAction.setTextColorRes(R.color.button_text_accent)
            }
            MaxActionAvailability.NotAvailable -> {
                setOnClickListener(null)
                viewMaxAmountAction.setTextColorRes(R.color.text_primary)
            }
        }
    }
}
