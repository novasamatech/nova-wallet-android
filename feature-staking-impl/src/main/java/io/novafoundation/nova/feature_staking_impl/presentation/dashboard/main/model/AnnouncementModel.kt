package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.model

import io.novafoundation.nova.common.view.AlertView

data class AnnouncementModel(
    val stylePreset: AlertView.StylePreset,
    val description: String
)
