package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import android.os.Parcelable
import io.novafoundation.nova.analytics.StakingPortfolioSource
import io.novafoundation.nova.common.navigation.InterScreenCommunicator
import kotlinx.parcelize.Parcelize

/** Responds when a finished flow returns to a portfolio that is already open, as iOS `SubtensorPortfolioPresenter.reopen(from:)`. */
interface BittensorPortfolioReopenCommunicator :
    InterScreenCommunicator<BittensorPortfolioReopenCommunicator.EmptyRequest, BittensorPortfolioReopenCommunicator.Response> {

    @Parcelize
    object EmptyRequest : Parcelable

    @Parcelize
    class Response(val source: StakingPortfolioSource) : Parcelable
}
