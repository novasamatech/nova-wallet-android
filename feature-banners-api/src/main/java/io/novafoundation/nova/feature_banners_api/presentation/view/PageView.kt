package io.novafoundation.nova.feature_banners_api.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.view.ClipableImageView
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

    val image: ClipableImageView
        get() = pagerBannerImage

    init {
        View.inflate(context, R.layout.view_pager_banner_page, this)
    }
}
