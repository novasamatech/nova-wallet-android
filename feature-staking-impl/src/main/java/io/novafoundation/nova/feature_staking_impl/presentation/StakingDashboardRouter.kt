package io.novafoundation.nova.feature_staking_impl.presentation

import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.utils.Event

interface StakingDashboardRouter {

    val scrollToDashboardTopEvent: LiveData<Event<Unit>>

    fun backInStakingTab()

    fun openMoreStakingOptions()

    fun returnToStakingDashboard()

    fun openStakingDashboard()
}
