package io.novafoundation.nova.feature_crowdloan_impl.presentation.contributions.model

import io.novafoundation.nova.common.utils.images.Icon

data class ContributionModel(
    val title: String,
    val amount: String,
    val icon: Icon,
)
