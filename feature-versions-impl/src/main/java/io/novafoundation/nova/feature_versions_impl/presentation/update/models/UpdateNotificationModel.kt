package io.novafoundation.nova.feature_versions_impl.presentation.update.models

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

class UpdateNotificationBannerModel(
    @DrawableRes val iconRes: Int,
    @DrawableRes val backgroundRes: Int,
    val title: String,
    val message: String
)

class UpdateNotificationModel(
    val version: String,
    val changelog: CharSequence?,
    val isLatestUpdate: Boolean,
    val severity: String?,
    @ColorRes val severityColorRes: Int?,
    @ColorRes val severityBackgroundRes: Int?,
    val date: String
)
