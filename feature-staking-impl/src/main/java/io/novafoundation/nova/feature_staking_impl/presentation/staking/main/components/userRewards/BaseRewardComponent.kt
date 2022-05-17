package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import kotlinx.coroutines.CoroutineScope

abstract class BaseRewardComponent(hostContext: ComponentHostContext) :
    UserRewardsComponent,
    CoroutineScope by hostContext.scope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(hostContext.scope) {

    override val events = MutableLiveData<Event<UserRewardsEvent>>()

    override fun onAction(action: UserRewardsAction) {}
}
