package io.novafoundation.nova.common.view.banner

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import io.novafoundation.nova.common.R
import kotlinx.android.synthetic.main.view_pager_banner_title_subtitle.view.pagerBannerSubtitle
import kotlinx.android.synthetic.main.view_pager_banner_title_subtitle.view.pagerBannerTitle

class BannerTitleSubtitleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    val title: TextView
        get() = pagerBannerTitle

    val subtitle: TextView
        get() = pagerBannerSubtitle

    init {
        View.inflate(context, R.layout.view_pager_banner_title_subtitle, this)
        orientation = VERTICAL
    }
}
