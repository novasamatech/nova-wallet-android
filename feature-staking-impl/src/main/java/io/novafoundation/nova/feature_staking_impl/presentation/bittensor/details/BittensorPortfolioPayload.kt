package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import android.os.Parcelable
import io.novafoundation.nova.analytics.StakingPortfolioSource
import kotlinx.parcelize.Parcelize

@Parcelize
data class BittensorPortfolioPayload(val source: StakingPortfolioSource) : Parcelable
