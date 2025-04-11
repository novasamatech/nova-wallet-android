package io.novafoundation.nova.feature_wallet_api.presentation.view.amount

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.databinding.ViewMaxAmountBinding
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.MaxActionAvailability
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.MaxAvailableView

class MaxAmountView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle), MaxAvailableView {

    private val binder = ViewMaxAmountBinding.inflate(inflater(), this)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    override fun setMaxAmountDisplay(maxAmountDisplay: String?) = letOrHide(maxAmountDisplay) { display ->
        binder.viewMaxAmountValue.text = display
    }

    override fun setMaxActionAvailability(availability: MaxActionAvailability) {
        when (availability) {
            is MaxActionAvailability.Available -> {
                setOnClickListener(availability.onMaxClicked)
                binder.viewMaxAmountAction.setTextColorRes(R.color.button_text_accent)
            }

            MaxActionAvailability.NotAvailable -> {
                setOnClickListener(null)
                binder.viewMaxAmountAction.setTextColorRes(R.color.text_primary)
            }
        }
    }
}
