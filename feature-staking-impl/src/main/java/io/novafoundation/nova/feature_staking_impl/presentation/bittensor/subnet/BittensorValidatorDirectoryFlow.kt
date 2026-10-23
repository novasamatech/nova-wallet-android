package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorDirectory
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

/**
 * The validator list as iOS SubtensorValidatorSelectPresenter provides it: with Hide unusual APY it waits for the recommendations
 * and fails with them; without it the directory shows as soon as it loads, and a failed ranking only leaves it unranked.
 */
internal fun observeBittensorValidatorDirectory(
    retryController: BittensorRetryController,
    hideUnusualApy: Flow<Boolean>,
    loadDirectory: suspend () -> BittensorValidatorDirectory,
    rankDirectory: suspend (BittensorValidatorDirectory) -> BittensorValidatorDirectory
): Flow<ExtendedLoadingState<BittensorValidatorDirectory>> {
    val directory = retryController.observe(
        step = "validator directory",
        transform = { it.withSafeLoading() },
        source = { flowOf { loadDirectory() } }
    )
    return directory.flatMapLatest { state ->
        val unranked = state.dataOrNull
        if (unranked == null) {
            kotlinx.coroutines.flow.flowOf(state)
        } else {
            val ranking = retryController.observe(
                step = "validator recommendations",
                transform = { it.withSafeLoading() },
                source = { flowOf { rankDirectory(unranked) } }
            )
            combine(ranking, hideUnusualApy) { ranking, hidesUnusualApy ->
                if (ranking is ExtendedLoadingState.Loaded || hidesUnusualApy) ranking else ExtendedLoadingState.Loaded(unranked)
            }
        }
    }
}
