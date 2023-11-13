package io.novafoundation.nova.feature_assets.presentation.model

import android.text.TextUtils
import androidx.annotation.ColorRes
import io.novafoundation.nova.common.utils.images.Icon

class OperationModel(
    val id: String,
    val amount: String,
    val amountDetails: String?,
    @ColorRes val amountColorRes: Int,
    val header: String,
    val statusAppearance: OperationStatusAppearance,
    val operationIcon: Icon,
    val subHeader: CharSequence,
    val subHeaderEllipsize: TextUtils.TruncateAt
)
