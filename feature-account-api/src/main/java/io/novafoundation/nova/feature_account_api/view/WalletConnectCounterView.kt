package io.novafoundation.nova.feature_account_api.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.view.isVisible
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawableFromColors
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.databinding.ViewWalletConnectBinding

class WalletConnectCounterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binder = ViewWalletConnectBinding.inflate(inflater(), this)

    init {
        binder.viewWalletConnectIconContainer.background = context.addRipple(
            drawable = context.getRoundedCornerDrawable(
                fillColorRes = R.color.button_wallet_connect_background,
                cornerSizeInDp = 80,
            ),
            mask = context.getRoundedCornerDrawableFromColors(Color.WHITE, strokeColor = null, cornerSizeInDp = 80)
        )
        orientation = HORIZONTAL
    }

    fun setConnectionCount(count: String?) {
        binder.viewWalletConnectConnectedCount.text = count
        binder.viewWalletConnectConnectedCount.isVisible = count != null
        binder.viewWalletConnectConnectionsIcon.isVisible = count != null

        background = if (count != null) {
            context.getRoundedCornerDrawable(
                fillColorRes = R.color.wallet_connections_background,
                cornerSizeInDp = 80,
            )
        } else {
            null
        }
    }
}
