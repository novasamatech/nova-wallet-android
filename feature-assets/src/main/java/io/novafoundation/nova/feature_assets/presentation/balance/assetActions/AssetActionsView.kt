package io.novafoundation.nova.feature_assets.presentation.balance.assetActions

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.updatePadding
import io.novafoundation.nova.common.view.shape.getBlockDrawable

class AssetActionsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    init {
        orientation = HORIZONTAL

        View.inflate(context, R.layout.view_asset_actions, this)

        background = context.getBlockDrawable()

        updatePadding(top = 4.dp(context), bottom = 4.dp(context))
    }

    val send: TextView
        get() = assetActionsSend

    val receive: TextView
        get() = assetActionsReceive

    val swap: TextView
        get() = assetActionsSwap

    val buy: TextView
        get() = assetActionsBuy
}
