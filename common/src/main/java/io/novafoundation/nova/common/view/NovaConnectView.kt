package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.useAttributes

class NovaConnectView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    init {
        orientation = HORIZONTAL

        View.inflate(context, R.layout.view_nova_connect, this)

        attrs?.let(::applyAttributes)
    }

    fun setTargetImage(@DrawableRes targetImageRes: Int) {
        viewNovaConnectTargetIcon.setImageResource(targetImageRes)
    }

    private fun applyAttributes(attributeSet: AttributeSet) = context.useAttributes(attributeSet, R.styleable.NovaConnectView) {
        val targetImage = it.getDrawable(R.styleable.NovaConnectView_targetImage)
        viewNovaConnectTargetIcon.setImageDrawable(targetImage)
    }
}
