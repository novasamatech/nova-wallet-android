package io.novafoundation.nova.feature_crowdloan_impl.presentation.contributions.model

import io.novafoundation.nova.feature_crowdloan_impl.presentation.model.Icon

data class ContributionModel(
    val name: String,
    val amount: String,
    val icon: Icon,
)
