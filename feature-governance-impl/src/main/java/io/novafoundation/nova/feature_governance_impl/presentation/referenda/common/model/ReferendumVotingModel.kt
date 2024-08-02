package io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

data class ReferendumVotingModel(
    val positiveFraction: Float?,
    val thresholdFraction: Float?,
    @DrawableRes val votingResultIcon: Int,
    @ColorRes val votingResultIconColor: Int,
    val thresholdInfo: String?,
    val thresholdInfoVisible: Boolean,
    val positivePercentage: String,
    val negativePercentage: String,
    val thresholdPercentage: String?,
)
