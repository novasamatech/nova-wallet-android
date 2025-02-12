package io.novafoundation.nova.feature_banners_api.presentation.view

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.utils.RoundCornersOutlineProvider
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.feature_banners_api.R
import kotlinx.android.synthetic.main.view_pager_banner_page.view.pagerBannerImage
import kotlinx.android.synthetic.main.view_pager_banner_page.view.pagerBannerSubtitle
import kotlinx.android.synthetic.main.view_pager_banner_page.view.pagerBannerTitle

class PageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    val title: TextView
        get() = pagerBannerTitle

    val subtitle: TextView
        get() = pagerBannerSubtitle

    val image: ImageView
        get() = pagerBannerImage

    private val roundCornersOutlineProvider = RoundCornersOutlineProvider(12.dpF)

    init {
        View.inflate(context, R.layout.view_pager_banner_page, this)
        outlineProvider = roundCornersOutlineProvider
        clipToOutline = true
    }

    fun setClipMargin(rect: Rect) {
        roundCornersOutlineProvider.setMargin(rect)
        invalidateOutline()
    }
}
