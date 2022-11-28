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
    @ColorRes val statusIconTint: Int,
    @ColorRes val statusTextTint: Int,
    @ColorRes val amountTint: Int
) {
    COMPLETED(R.drawable.ic_checkmark_circle_16, R.string.transaction_status_completed, R.color.icon_positive, R.color.text_positive, R.color.text_primary),
    PENDING(R.drawable.ic_time_16, R.string.transaction_status_pending, R.color.icon_secondary, R.color.text_secondary, R.color.text_primary),
    FAILED(R.drawable.ic_red_cross, R.string.transaction_status_failed, R.color.icon_negative, R.color.text_negative, R.color.text_secondary),
}

fun TextView.showOperationStatus(statusAppearance: OperationStatusAppearance) {
    setText(statusAppearance.labelRes)
    setDrawableStart(
        drawableRes = statusAppearance.icon,
        widthInDp = 16,
        paddingInDp = 4,
        tint = statusAppearance.statusIconTint
    )
    setTextColorRes(statusAppearance.statusTextTint)
}
