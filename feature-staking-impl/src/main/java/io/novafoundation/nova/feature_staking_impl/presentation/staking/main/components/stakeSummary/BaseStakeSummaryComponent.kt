package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions

abstract class BaseStakeSummaryComponent(
    scope: ComputationalScope
) : StakeSummaryComponent,
    ComputationalScope by scope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(scope) {

    override val events = MutableLiveData<Event<StakeSummaryEvent>>()

    override fun onAction(action: StakeSummaryAction) {}
}
