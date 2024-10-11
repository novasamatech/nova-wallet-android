package io.novafoundation.nova.feature_swap_api.presentation.view

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.feature_swap_api.R

class SwapAssetsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        inflate(context, R.layout.view_swap_assets, this)
    }

    fun setModel(model: Model) {
        setAssetIn(model.assetIn)
        setAssetOut(model.assetOut)
    }

    fun setAssetIn(model: SwapAssetView.Model) {
        viewSwapAssetsIn.setModel(model)
    }

    fun setAssetOut(model: SwapAssetView.Model) {
        viewSwapAssetsOut.setModel(model)
    }

    class Model(
        val assetIn: SwapAssetView.Model,
        val assetOut: SwapAssetView.Model
    )
}
