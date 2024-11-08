package io.novafoundation.nova.feature_governance_impl.presentation.unlock.list.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.utils.formatting.TimerValue

data class GovernanceLockModel(
    val index: Int,
    val amount: CharSequence,
    val status: StatusContent,
    @ColorRes val statusColorRes: Int,
    @DrawableRes val statusIconRes: Int?,
    @ColorRes val statusIconColorRes: Int?
) {

    sealed class StatusContent {

        data class Timer(val timer: TimerValue) : StatusContent()

        data class Text(val text: String) : StatusContent()
    }
}
