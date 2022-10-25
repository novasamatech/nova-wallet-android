package io.novafoundation.nova.feature_governance_impl.presentation.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import coil.ImageLoader
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.updatePadding
import io.novafoundation.nova.feature_governance_impl.R
import kotlinx.android.synthetic.main.view_chip.view.chipIcon
import kotlinx.android.synthetic.main.view_chip.view.chipText

class NovaChipView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle), WithContextExtensions by WithContextExtensions(context) {

    init {
        View.inflate(context, R.layout.view_chip, this)
        orientation = HORIZONTAL

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.NovaChipView)

        if (typedArray.hasValue(R.styleable.NovaChipView_chipIcon)) {
            val iconDrawable = typedArray.getDrawable(R.styleable.NovaChipView_chipIcon)
            setIcon(iconDrawable)
        } else {
            setIcon(null)
        }

        val text = typedArray.getString(R.styleable.NovaChipView_android_text)
        setText(text)

        val backgroundTintColor = typedArray.getResourceId(R.styleable.NovaChipView_backgroundColor, R.color.white_8)
        background = getRoundedCornerDrawable(backgroundTintColor, cornerSizeDp = 8)
            .withRippleMask(getRippleMask(cornerSizeDp = 8))

        typedArray.recycle()
    }

    fun setIcon(icon: Icon?, imageLoader: ImageLoader) {
        icon?.let { chipIcon.setIcon(icon, imageLoader) }
        useIcon(icon != null)
    }

    fun setIcon(drawable: Drawable?) {
        chipIcon.setImageDrawable(drawable)
        useIcon(drawable != null)
    }

    fun setText(text: String?) {
        chipText.text = text
    }

    private fun useIcon(useIcon: Boolean) {
        chipIcon.isVisible = useIcon

        val startPadding = if (useIcon) 6 else 8
        updatePadding(start = startPadding.dp)
    }
}
