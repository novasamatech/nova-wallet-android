package io.novafoundation.nova.feature_crowdloan_impl.presentation.contributions.model

import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
data class ContributionModel(
    val title: String,
    val amount: AmountModel,
    val icon: Icon,
    val returnsIn: TimerValue,
)
