package io.novafoundation.nova.feature_banners_api.presentation.view

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.utils.RoundCornersOutlineProvider
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_banners_api.databinding.ViewPagerBannerPageBinding

class PageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binder = ViewPagerBannerPageBinding.inflate(inflater(), this)

    val title: TextView
        get() = binder.pagerBannerTitle

    val subtitle: TextView
        get() = binder.pagerBannerSubtitle

    val image: ImageView
        get() = binder.pagerBannerImage

    private val roundCornersOutlineProvider = RoundCornersOutlineProvider(12.dpF)

    init {
        outlineProvider = roundCornersOutlineProvider
        clipToOutline = true
    }

    fun setClipMargin(rect: Rect) {
        roundCornersOutlineProvider.setMargin(rect)
        invalidateOutline()
    }
}
