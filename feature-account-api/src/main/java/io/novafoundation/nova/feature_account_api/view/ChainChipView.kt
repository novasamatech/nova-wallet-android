package io.novafoundation.nova.feature_account_api.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import coil.ImageLoader
import coil.load
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.getAccentColorRes
import io.novafoundation.nova.common.utils.setDrawableEnd
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_account_api.presenatation.chain.toDrawable
import kotlinx.android.synthetic.main.view_chain_chip.view.itemAssetGroupLabel
import kotlinx.android.synthetic.main.view_chain_chip.view.itemAssetGroupLabelIcon

class ChainChipModel(
    val chainUi: ChainUi,
    val changeable: Boolean
)

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

    fun setModel(chainChipModel: ChainChipModel) {
        setChain(chainChipModel.chainUi)
        setChangeable(chainChipModel.changeable)
    }

    fun setChangeable(changeable: Boolean) {
        itemAssetGroupLabel.isEnabled = changeable

        if (changeable) {
            val accentColorRes = context.getAccentColorRes()

            itemAssetGroupLabel.setTextColorRes(accentColorRes)
            itemAssetGroupLabel.setDrawableEnd(R.drawable.ic_chevron_down, widthInDp = 16, paddingInDp = 4, tint = accentColorRes)
        } else {
            itemAssetGroupLabel.setTextColorRes(R.color.white)
            itemAssetGroupLabel.setDrawableEnd(null)
        }
    }

    fun setChain(chainUi: ChainUi) {
        itemAssetGroupLabel.text = chainUi.name

        itemAssetGroupLabelIcon.load(chainUi.icon, imageLoader)
        itemAssetGroupLabelIcon.background = chainUi.gradient.toDrawable(context)
    }
}
