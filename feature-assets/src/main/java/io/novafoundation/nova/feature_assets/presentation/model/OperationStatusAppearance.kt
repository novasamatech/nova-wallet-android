package io.novafoundation.nova.feature_assets.presentation.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.novafoundation.nova.feature_assets.R

enum class OperationStatusAppearance(
    @DrawableRes val icon: Int,
    @StringRes val labelRes: Int,
) {
    COMPLETED(R.drawable.ic_transaction_completed, R.string.transaction_status_completed),
    PENDING(R.drawable.ic_transaction_pending, R.string.transaction_status_pending),
    FAILED(R.drawable.ic_red_cross, R.string.transaction_status_failed),
}
