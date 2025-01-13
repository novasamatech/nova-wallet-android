package io.novafoundation.nova.feature_assets.presentation.balance.assetActions

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.updatePadding
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.feature_assets.databinding.ViewAssetActionsBinding

class AssetActionsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    private val binder = ViewAssetActionsBinding.inflate(inflater(), this)

    init {
        orientation = HORIZONTAL

        background = context.getBlockDrawable()

        updatePadding(top = 4.dp(context), bottom = 4.dp(context))
    }

    val send: TextView
        get() = binder.assetActionsSend

    val receive: TextView
        get() = binder.assetActionsReceive

    val swap: TextView
        get() = binder.assetActionsSwap

    val buy: TextView
        get() = binder.assetActionsBuy
}
