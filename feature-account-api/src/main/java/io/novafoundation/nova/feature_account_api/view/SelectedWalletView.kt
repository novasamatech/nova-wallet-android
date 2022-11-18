package io.novafoundation.nova.feature_account_api.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedWalletModel
import kotlinx.android.synthetic.main.view_selected_wallet.view.viewSelectedWalletAccountIcon
import kotlinx.android.synthetic.main.view_selected_wallet.view.viewSelectedWalletTypeIcon

class SelectedWalletView @kotlin.jvm.JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        View.inflate(context, R.layout.view_selected_wallet, this)

        orientation = HORIZONTAL
    }

    fun setModel(model: SelectedWalletModel) {
        viewSelectedWalletAccountIcon.setImageDrawable(model.walletIcon)

        if (model.typeIcon != null) {
            background = context.getRoundedCornerDrawable(
                fillColorRes = R.color.white_16,
                strokeColorRes = R.color.divider,
                cornerSizeInDp = 80,
            )

            viewSelectedWalletTypeIcon.setImageResource(model.typeIcon)
            viewSelectedWalletTypeIcon.makeVisible()
        } else {
            background = null
            viewSelectedWalletTypeIcon.makeGone()
        }
    }
}
