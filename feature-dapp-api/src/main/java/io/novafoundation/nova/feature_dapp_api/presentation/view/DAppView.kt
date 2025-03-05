package io.novafoundation.nova.feature_dapp_api.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import coil.ImageLoader
import coil.clear
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.updatePadding
import io.novafoundation.nova.feature_dapp_api.R
import io.novafoundation.nova.feature_external_sign_api.presentation.dapp.showDAppIcon
import kotlinx.android.synthetic.main.view_dapp.view.itemDAppIcon
import kotlinx.android.synthetic.main.view_dapp.view.itemDAppSubtitle
import kotlinx.android.synthetic.main.view_dapp.view.itemDAppSubtitleIcon
import kotlinx.android.synthetic.main.view_dapp.view.itemDAppTitle
import kotlinx.android.synthetic.main.view_dapp.view.itemDappAction
import kotlinx.android.synthetic.main.view_dapp.view.itemDappFavorite

class DAppView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle) {

    companion object {
        fun createUsingMathParentWidth(context: Context): DAppView {
            return DAppView(context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
        }
    }

    private val imageLoader: ImageLoader by lazy(LazyThreadSafetyMode.NONE) {
        FeatureUtils.getCommonApi(context).imageLoader()
    }

    init {
        View.inflate(context, R.layout.view_dapp, this)
        setBackgroundResource(R.drawable.bg_primary_list_item)
    }

    fun setTitle(name: String?) {
        itemDAppTitle.text = name
    }

    fun showTitle(show: Boolean) {
        itemDAppTitle.setVisible(show)
    }

    fun setSubtitle(url: String?) {
        itemDAppSubtitle.text = url
    }

    fun showSubtitle(show: Boolean) {
        itemDAppSubtitle.setVisible(show)
    }

    fun setIconUrl(iconUrl: String?) {
        itemDAppIcon.showDAppIcon(iconUrl, imageLoader)
    }

    fun setFavoriteIconVisible(visible: Boolean) {
        itemDappFavorite.setVisible(visible)
    }

    fun enableSubtitleIcon(): ImageView {
        return itemDAppSubtitleIcon.also { icon -> icon.makeVisible() }
    }

    fun setOnActionClickListener(listener: OnClickListener?) {
        itemDappAction.setOnClickListener(listener)
    }

    fun setActionResource(@DrawableRes iconRes: Int?, @ColorRes colorRes: Int? = null) {
        if (iconRes == null) {
            itemDappAction.setImageDrawable(null)
        } else {
            itemDappAction.setImageResource(iconRes)
            itemDappAction.setImageTintRes(colorRes)
        }
    }

    fun setActionTintRes(@ColorRes color: Int?) {
        itemDappAction.setImageTintRes(color)
    }

    fun setActionEndPadding(rightPadding: Int) {
        itemDappAction.updatePadding(end = rightPadding)
    }

    fun clearIcon() {
        itemDAppIcon.clear()
    }
}
