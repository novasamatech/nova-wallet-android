package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.presentation.dataOrNull
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.firstNotNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class BaseStakeSummaryComponent(
    scope: CoroutineScope
) : StakeSummaryComponent,
    CoroutineScope by scope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(scope) {

    override val events = MutableLiveData<Event<StakeSummaryEvent>>()

    override fun onAction(action: StakeSummaryAction) {}
}
