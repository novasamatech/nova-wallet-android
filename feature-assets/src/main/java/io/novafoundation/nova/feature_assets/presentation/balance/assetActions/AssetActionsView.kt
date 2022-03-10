package io.novafoundation.nova.feature_assets.presentation.balance.assetActions

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.updatePadding
import io.novafoundation.nova.common.view.shape.getBlurDrawable
import kotlinx.android.synthetic.main.view_asset_actions.view.assetActionsBuy
import kotlinx.android.synthetic.main.view_asset_actions.view.assetActionsReceive
import kotlinx.android.synthetic.main.view_asset_actions.view.assetActionsSend

class AssetActionsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    init {
        orientation = HORIZONTAL

        View.inflate(context, R.layout.view_asset_actions, this)

        background = context.getBlurDrawable()

        updatePadding(top = 4.dp(context), bottom = 4.dp(context))
    }

    val send: TextView
        get() = assetActionsSend

    val receive: TextView
        get() = assetActionsReceive

    val buy: TextView
        get() = assetActionsBuy
}
