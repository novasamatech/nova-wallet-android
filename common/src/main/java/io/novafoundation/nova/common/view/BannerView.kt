package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.common.utils.getEnum
import kotlinx.android.synthetic.main.view_banner.view.bannerBackground
import kotlinx.android.synthetic.main.view_banner.view.bannerClose
import kotlinx.android.synthetic.main.view_banner.view.bannerContent
import kotlinx.android.synthetic.main.view_banner.view.bannerImage

class BannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : MaterialCardView(context, attrs, defStyle) {

    init {
        View.inflate(context, R.layout.view_banner, this)
        cardElevation = 0f
        radius = 12f.dpF(context)
        strokeWidth = 1.dp(context)
        strokeColor = context.getColor(R.color.container_border)

        applyAttributes(attrs)
    }

    fun setOnCloseClickListener(listener: OnClickListener?) {
        bannerClose.setOnClickListener(listener)
    }

    private fun applyAttributes(attrs: AttributeSet?) {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.BannerView)

            val image = typedArray.getDrawable(R.styleable.BannerView_android_src)
            bannerImage.setImageDrawable(image)

            val background = typedArray.getDrawable(R.styleable.BannerView_bannerBackground)
            bannerBackground.background = background

            val closeIcon = typedArray.getDrawable(R.styleable.BannerView_closeIcon)
            if (closeIcon != null) {
                bannerClose.setImageDrawable(closeIcon)
            }

            val showClose = typedArray.getBoolean(R.styleable.BannerView_showClose, false)
            bannerClose.isVisible = showClose

            val style = typedArray.getEnum(R.styleable.BannerView_android_scaleType, ImageView.ScaleType.CENTER)
            setImageScaleType(style)

            typedArray.recycle()
        }
    }

    override fun addView(child: View, params: ViewGroup.LayoutParams?) {
        if (child.id == R.id.bannerBackground) {
            super.addView(child, params)
        } else {
            bannerContent.addView(child, params)
        }
    }

    fun setBannerBackground(@DrawableRes backgroundRes: Int) {
        bannerBackground.setBackgroundResource(backgroundRes)
    }

    fun setImage(@DrawableRes imageRes: Int) {
        bannerImage.setImageResource(imageRes)
    }

    fun setImageScaleType(scaleType: ImageView.ScaleType) {
        bannerImage.scaleType = scaleType
    }
}
