package io.novafoundation.nova.feature_account_api.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import coil.ImageLoader
import coil.load
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.common.view.shape.gradientDrawable
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import kotlinx.android.synthetic.main.view_chain_chip.view.itemAssetGroupLabel
import kotlinx.android.synthetic.main.view_chain_chip.view.itemAssetGroupLabelIcon

class ChainChipView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val imageLoader: ImageLoader by lazy(LazyThreadSafetyMode.NONE) {
        FeatureUtils.getCommonApi(context).imageLoader()
    }

    init {
        View.inflate(context, R.layout.view_chain_chip, this)

        itemAssetGroupLabel.background = context.getRoundedCornerDrawable(R.color.white_16, cornerSizeInDp = 8)
    }

    fun setChain(chainUi: ChainUi) {
        itemAssetGroupLabel.text = chainUi.name

        itemAssetGroupLabelIcon.load(chainUi.icon, imageLoader)
        itemAssetGroupLabelIcon.background = context.gradientDrawable(
            colors = chainUi.gradient.colors,
            offsets = chainUi.gradient.positions,
            angle = chainUi.gradient.angle,
            cornerRadiusDp = 8
        )
    }
}
