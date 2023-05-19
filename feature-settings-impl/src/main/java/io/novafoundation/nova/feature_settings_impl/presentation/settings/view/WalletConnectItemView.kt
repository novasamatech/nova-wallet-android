package io.novafoundation.nova.feature_settings_impl.presentation.settings.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.getDrawableCompat
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.themed
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.presentation.settings.model.WalletConnectSessionsModel
import kotlinx.android.synthetic.main.view_wallet_connect_item.view.walletConnectItemValue

class WalletConnectItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        View.inflate(context, R.layout.view_wallet_connect_item, this)

        orientation = HORIZONTAL
        background = context.getDrawableCompat(R.drawable.bg_primary_list_item)
    }

    fun setValue(value: WalletConnectSessionsModel) {
        walletConnectItemValue.setModel(value)
    }
}

class WalletConnectConnectionsChip @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatTextView(context.themed(R.style.TextAppearance_NovaFoundation_Regular_Footnote), attrs, defStyleAttr),
    WithContextExtensions by WithContextExtensions(context) {

    init {
        setTextColorRes(R.color.chip_text)
        setDrawableStart(R.drawable.ic_connections, widthInDp = 12, paddingInDp = 4, tint = R.color.chip_icon)

        gravity = Gravity.CENTER_VERTICAL
        includeFontPadding = false

        setPadding(8.dp, 4.dp, 8.dp, 4.dp)

        background = getRoundedCornerDrawable(R.color.chips_background, cornerSizeDp = 8)
    }
}

fun WalletConnectConnectionsChip.setModel(model: WalletConnectSessionsModel) {
    setTextOrHide(model.connections)
}
