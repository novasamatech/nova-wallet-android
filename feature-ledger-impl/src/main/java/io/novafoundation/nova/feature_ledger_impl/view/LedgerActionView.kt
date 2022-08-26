package io.novafoundation.nova.feature_ledger_impl.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.getDrawableCompat
import io.novafoundation.nova.common.utils.getResourceIdOrNull
import io.novafoundation.nova.common.utils.setDrawableTop
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.feature_ledger_impl.R
import kotlinx.android.synthetic.main.view_ledger_action.view.viewLedgerActionMessage

class LedgerActionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), WithContextExtensions by WithContextExtensions(context) {

    init {
        View.inflate(context, R.layout.view_ledger_action, this)

        attrs?.let(::applyAttributes)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.makeMeasureSpec(210.dp, MeasureSpec.EXACTLY)
        val height = MeasureSpec.makeMeasureSpec(190.dp, MeasureSpec.EXACTLY)

        super.onMeasure(width, height)
    }

    fun setMessage(message: String?) {
        viewLedgerActionMessage.text = message
    }

    fun setMessage(@StringRes messageRes: Int) {
        viewLedgerActionMessage.setText(messageRes)
    }

    fun setIcon(@DrawableRes drawableResId: Int?) {
        viewLedgerActionMessage.setDrawableTop(drawableResId, widthInDp = 14, paddingInDp = 1, tint = R.color.white_64)
    }

    fun setLedgerImage(image: Drawable) {
        background = image
    }

    private fun applyAttributes(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.LedgerActionView) { typedArray ->
        val message = typedArray.getString(R.styleable.LedgerActionView_la_message)
        setMessage(message)

        val icon = typedArray.getResourceIdOrNull(R.styleable.LedgerActionView_la_icon)
        setIcon(icon)

        val ledgerImage = typedArray.getDrawable(R.styleable.LedgerActionView_la_ledgerImage)
            ?: context.getDrawableCompat(R.drawable.ic_ledger_info)
        setLedgerImage(ledgerImage)
    }
}
