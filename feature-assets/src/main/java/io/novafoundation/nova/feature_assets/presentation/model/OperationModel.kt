package io.novafoundation.nova.feature_assets.presentation.model

import androidx.annotation.ColorRes
import io.novafoundation.nova.common.utils.images.Icon

class OperationModel(
    val id: String,
    val amount: String,
    val fiatWithTime: String?,
    @ColorRes val amountColorRes: Int,
    val header: String,
    val statusAppearance: OperationStatusAppearance,
    val operationIcon: Icon,
    val subHeader: String
)
