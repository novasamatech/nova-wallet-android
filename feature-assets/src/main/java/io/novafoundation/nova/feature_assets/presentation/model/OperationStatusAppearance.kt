package io.novafoundation.nova.feature_assets.presentation.model

import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_assets.R

enum class OperationStatusAppearance(
    @DrawableRes val icon: Int,
    @StringRes val labelRes: Int,
    @ColorRes val statusTint: Int,
    @ColorRes val amountTint: Int
) {
    COMPLETED(R.drawable.ic_checkmark_circle_16, R.string.transaction_status_completed, R.color.green, R.color.white),
    PENDING(R.drawable.ic_time_16, R.string.transaction_status_pending, R.color.white_64, R.color.white_64),
    FAILED(R.drawable.ic_red_cross, R.string.transaction_status_failed, R.color.red, R.color.white_64),
}

fun TextView.showOperationStatus(statusAppearance: OperationStatusAppearance) {
    setText(statusAppearance.labelRes)
    setDrawableStart(
        drawableRes = statusAppearance.icon,
        widthInDp = 16,
        paddingInDp = 4,
        tint = statusAppearance.statusTint
    )
    setTextColorRes(statusAppearance.statusTint)
}
