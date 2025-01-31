package io.novafoundation.nova.feature_dapp_api.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import coil.ImageLoader
import coil.clear
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.updatePadding
import io.novafoundation.nova.feature_dapp_api.R
import io.novafoundation.nova.feature_dapp_api.databinding.ViewDappBinding
import io.novafoundation.nova.feature_external_sign_api.presentation.dapp.showDAppIcon

class DAppView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle) {

    private val binder = ViewDappBinding.inflate(inflater(), this)

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
        setBackgroundResource(R.drawable.bg_primary_list_item)
    }

    fun setTitle(name: String?) {
        binder.itemDAppTitle.text = name
    }

    fun showTitle(show: Boolean) {
        binder.itemDAppTitle.setVisible(show)
    }

    fun setSubtitle(url: String?) {
        binder.itemDAppSubtitle.text = url
    }

    fun showSubtitle(show: Boolean) {
        binder.itemDAppSubtitle.setVisible(show)
    }

    fun setIconUrl(iconUrl: String?) {
        binder.itemDAppIcon.showDAppIcon(iconUrl, imageLoader)
    }

    fun setFavoriteIconVisible(visible: Boolean) {
        binder.itemDappFavorite.setVisible(visible)
    }

    fun enableSubtitleIcon(): ImageView {
        return binder.itemDAppSubtitleIcon.also { icon -> icon.makeVisible() }
    }

    fun setOnActionClickListener(listener: OnClickListener?) {
        binder.itemDappAction.setOnClickListener(listener)
    }

    fun setActionResource(@DrawableRes iconRes: Int?, @ColorRes colorRes: Int? = null) {
        if (iconRes == null) {
            binder.itemDappAction.setImageDrawable(null)
        } else {
            binder.itemDappAction.setImageResource(iconRes)
            binder.itemDappAction.setImageTintRes(colorRes)
        }
    }

    fun setActionTintRes(@ColorRes color: Int?) {
        binder.itemDappAction.setImageTintRes(color)
    }

    fun setActionEndPadding(rightPadding: Int) {
        binder.itemDappAction.updatePadding(end = rightPadding)
    }

    fun clearIcon() {
        binder.itemDAppIcon.clear()
    }
}
