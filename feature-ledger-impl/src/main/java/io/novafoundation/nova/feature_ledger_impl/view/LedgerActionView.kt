package io.novafoundation.nova.feature_ledger_impl.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.getResourceIdOrNull
import io.novafoundation.nova.common.utils.setImageResource
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.feature_ledger_impl.R

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

    fun setIcon(@DrawableRes drawableResId: Int?, @ColorRes tint: Int? = null) {
        viewLedgerGraphicsIcon.setImageResource(drawableResId)
        viewLedgerGraphicsIcon.setImageTintRes(tint)
    }

    fun setLedgerImage(@DrawableRes imageRes: Int) {
        setBackgroundResource(imageRes)
    }

    private fun applyAttributes(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.LedgerActionView) { typedArray ->
        val icon = typedArray.getResourceIdOrNull(R.styleable.LedgerActionView_la_icon)
        setIcon(icon)

        val ledgerImage = typedArray.getResourceId(R.styleable.LedgerActionView_la_ledgerImage, R.drawable.ic_ledger_info)
        setLedgerImage(ledgerImage)
    }
}
