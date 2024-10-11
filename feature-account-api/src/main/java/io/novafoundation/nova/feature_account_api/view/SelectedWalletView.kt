package io.novafoundation.nova.feature_account_api.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedWalletModel

class SelectedWalletView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        View.inflate(context, R.layout.view_selected_wallet, this)
    }

    fun setModel(model: SelectedWalletModel) {
        viewSelectedWalletAccountIcon.setImageDrawable(model.walletIcon)

        viewSelectedWalletAccountUpdateIndicator.isVisible = model.hasUpdates

        if (model.typeIcon != null) {
            background = context.getRoundedCornerDrawable(
                fillColorRes = R.color.chips_background,
                cornerSizeInDp = 80,
            )

            viewSelectedWalletTypeIcon.setImageResource(model.typeIcon.icon)
            val tint = R.color.icon_primary.takeIf { model.typeIcon.canApplyOwnTint }
            viewSelectedWalletTypeIcon.setImageTintRes(tint)

            viewSelectedWalletTypeIcon.makeVisible()
        } else {
            background = null
            viewSelectedWalletTypeIcon.makeGone()
        }
    }
}
