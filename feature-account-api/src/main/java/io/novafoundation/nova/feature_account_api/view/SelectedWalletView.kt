package io.novafoundation.nova.feature_account_api.view

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.databinding.ViewSelectedWalletBinding
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedWalletModel

class SelectedWalletView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binder = ViewSelectedWalletBinding.inflate(inflater(), this)

    fun setModel(model: SelectedWalletModel) {
        binder.viewSelectedWalletAccountIcon.setImageDrawable(model.walletIcon)

        binder.viewSelectedWalletAccountUpdateIndicator.isVisible = model.hasUpdates

        if (model.typeIcon != null) {
            background = context.getRoundedCornerDrawable(
                fillColorRes = R.color.chips_background,
                cornerSizeInDp = 80,
            )

            binder.viewSelectedWalletTypeIcon.setImageResource(model.typeIcon.icon)
            val tint = R.color.icon_primary.takeIf { model.typeIcon.canApplyOwnTint }
            binder.viewSelectedWalletTypeIcon.setImageTintRes(tint)

            binder.viewSelectedWalletTypeIcon.makeVisible()
        } else {
            background = null
            binder.viewSelectedWalletTypeIcon.makeGone()
        }
    }
}
