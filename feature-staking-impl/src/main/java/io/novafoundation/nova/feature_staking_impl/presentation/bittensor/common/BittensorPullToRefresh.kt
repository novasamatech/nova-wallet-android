package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Pull-to-refresh for a Bittensor screen.
 *
 * [refresh] restarts every read the screen observes through its [BittensorRetryController]. The indicator stays
 * on until [primary], the read the screen is built around, answers again with data or a failure; secondary reads
 * keep loading behind the refreshed content.
 */
class BittensorPullToRefresh(
    private val retryController: BittensorRetryController,
    primary: Flow<ExtendedLoadingState<*>>,
    scope: CoroutineScope
) {

    private val refreshing = MutableStateFlow(false)

    val isRefreshing: Flow<Boolean> = refreshing

    init {
        primary
            .onEach { state -> if (state !is ExtendedLoadingState.Loading) refreshing.value = false }
            .launchIn(scope)
    }

    fun refresh() {
        refreshing.value = true
        retryController.reload()
    }
}
