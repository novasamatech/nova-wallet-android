package io.novafoundation.nova.feature_swap_impl.presentation.common.route

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.updateMargins
import coil.ImageLoader
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadChainIcon
import io.novafoundation.nova.feature_swap_impl.R

class SwapRouteView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val imageLoader: ImageLoader by lazy(LazyThreadSafetyMode.NONE) {
        FeatureUtils.getCommonApi(context).imageLoader()
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    fun setModel(model: SwapRouteModel) {
        removeAllViews()

        addViewsFor(model)
    }

    private fun addViewsFor(model: SwapRouteModel) {
        model.chains.forEachIndexed { index, chainUi ->
            val hasNext = index < model.chains.size - 1

            addChainView(chainUi)
            if (hasNext) {
                addArrow()
            }
        }
    }

    private fun addChainView(chainUi: ChainUi) {
        ImageView(context).apply {
            layoutParams = LayoutParams(16.dp, 16.dp)

            loadChainIcon(chainUi.icon, imageLoader)
        }.also(::addView)
    }

    private fun addArrow() {
        ImageView(context).apply {
            layoutParams = LayoutParams(12.dp, 12.dp).apply {
                updateMargins(left = 4.dp, right = 4.dp)
            }

            setImageResource(R.drawable.ic_arrow_right)
            setImageTintRes(R.color.icon_secondary)
        }.also(::addView)
    }
}
