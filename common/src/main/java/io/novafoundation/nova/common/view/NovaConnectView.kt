package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.ViewNovaConnectBinding
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.useAttributes

class NovaConnectView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    private val binder = ViewNovaConnectBinding.inflate(inflater(), this)

    init {
        orientation = HORIZONTAL

        attrs?.let(::applyAttributes)
    }

    fun setTargetImage(@DrawableRes targetImageRes: Int) {
        binder.viewNovaConnectTargetIcon.setImageResource(targetImageRes)
    }

    private fun applyAttributes(attributeSet: AttributeSet) = context.useAttributes(attributeSet, R.styleable.NovaConnectView) {
        val targetImage = it.getDrawable(R.styleable.NovaConnectView_targetImage)
        binder.viewNovaConnectTargetIcon.setImageDrawable(targetImage)
    }
}
