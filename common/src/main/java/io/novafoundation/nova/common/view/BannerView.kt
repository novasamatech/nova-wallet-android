package io.novafoundation.nova.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.google.android.material.card.MaterialCardView
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.dpF
import kotlinx.android.synthetic.main.view_banner.view.bannerBackground
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

    private fun applyAttributes(attrs: AttributeSet?) {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.BannerView)

            val image = typedArray.getDrawable(R.styleable.BannerView_android_src)
            bannerImage.setImageDrawable(image)

            val background = typedArray.getDrawable(R.styleable.BannerView_bannerBackground)
            bannerBackground.background = background

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
}
