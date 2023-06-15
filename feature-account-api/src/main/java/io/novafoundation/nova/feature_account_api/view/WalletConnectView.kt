package io.novafoundation.nova.feature_account_api.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawableFromColors
import io.novafoundation.nova.feature_account_api.R
import kotlinx.android.synthetic.main.view_wallet_connect.view.viewWalletConnectConnectedCount
import kotlinx.android.synthetic.main.view_wallet_connect.view.viewWalletConnectConnectionsIcon
import kotlinx.android.synthetic.main.view_wallet_connect.view.viewWalletConnectIconContainer

class WalletConnectView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        View.inflate(context, R.layout.view_wallet_connect, this)
        viewWalletConnectIconContainer.background = context.addRipple(
            drawable = context.getRoundedCornerDrawable(
                fillColorRes = R.color.button_wallet_connect_background,
                cornerSizeInDp = 80,
            ),
            mask = context.getRoundedCornerDrawableFromColors(Color.WHITE, strokeColor = null, cornerSizeInDp = 80)
        )
        orientation = HORIZONTAL
    }

    fun setConnectionCount(count: String?) {
        viewWalletConnectConnectedCount.text = count
        viewWalletConnectConnectedCount.isVisible = count != null
        viewWalletConnectConnectionsIcon.isVisible = count != null

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
