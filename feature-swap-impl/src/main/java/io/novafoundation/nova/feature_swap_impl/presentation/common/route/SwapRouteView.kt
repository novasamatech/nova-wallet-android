package io.novafoundation.nova.feature_swap_impl.presentation.common.route

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import androidx.core.view.updateMargins
import coil.ImageLoader
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_account_api.presenatation.chain.loadChainIcon
import io.novafoundation.nova.feature_swap_impl.R

private const val SHOW_CHAIN_NAMES_DEFAULT = false

class SwapRouteView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val imageLoader: ImageLoader by lazy(LazyThreadSafetyMode.NONE) {
        FeatureUtils.getCommonApi(context).imageLoader()
    }

    private var shouldShowChainNames: Boolean = SHOW_CHAIN_NAMES_DEFAULT

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        attrs?.let(::applyAttrs)
    }

    fun setModel(model: SwapRouteModel) {
        removeAllViews()

        addViewsFor(model)
    }

    fun setShowChainNames(showChainNames: Boolean) {
        shouldShowChainNames = showChainNames
    }

    private fun addViewsFor(model: SwapRouteModel) {
        model.chains.forEachIndexed { index, chainUi ->
            val hasNext = index < model.chains.size - 1

            addChainIcon(chainUi)
            if (shouldShowChainNames) {
                addChainName(chainUi)
            }

            if (hasNext) {
                addArrow()
            }
        }
    }

    private fun addChainIcon(chainUi: ChainUi) {
        ImageView(context).apply {
            layoutParams = LayoutParams(16.dp, 16.dp)

            loadChainIcon(chainUi.icon, imageLoader)
        }.also(::addView)
    }

    private fun addChainName(chainUi: ChainUi) {
        TextView(context).apply {
            layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                updateMargins(left = 8.dp)
            }

            ellipsize = TextUtils.TruncateAt.END
            setTextAppearance(R.style.TextAppearance_NovaFoundation_Regular_Footnote)
            setTextColorRes(R.color.text_primary)

            text = chainUi.name
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

    private fun applyAttrs(attributeSet: AttributeSet) = context.useAttributes(attributeSet, R.styleable.SwapRouteView) {
        val shouldShowChainNames = it.getBoolean(R.styleable.SwapRouteView_SwapRouteView_displayChainName, SHOW_CHAIN_NAMES_DEFAULT)
        setShowChainNames(shouldShowChainNames)
    }
}
