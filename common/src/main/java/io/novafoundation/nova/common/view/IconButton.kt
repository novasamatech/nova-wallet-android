package io.novafoundation.nova.common.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.updatePadding
import io.novafoundation.nova.common.utils.useAttributes

class IconButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr), WithContextExtensions by WithContextExtensions(context) {

    init {
        updatePadding(top = 6.dp, bottom = 6.dp, start = 12.dp, end = 12.dp)

        background = addRipple(getRoundedCornerDrawable(R.color.button_background_secondary))

        attrs?.let(::applyAttributes)
    }

    fun setIcon(icon: Drawable) {
        setImageDrawable(icon)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = MeasureSpec.makeMeasureSpec(32.dp, MeasureSpec.EXACTLY)
        val width = MeasureSpec.makeMeasureSpec(44.dp, MeasureSpec.EXACTLY)

        super.onMeasure(width, height)
    }

    private fun applyAttributes(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.IconButton) { typedArray ->
        val icon = typedArray.getDrawable(R.styleable.IconButton_android_src)
        icon?.let(::setIcon)
    }
}
