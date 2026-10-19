package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorDirectory
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorRetryController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

internal fun observeBittensorValidatorDirectory(
    retryController: BittensorRetryController,
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
            retryController.observe(
                step = "validator recommendations",
                transform = { it.withSafeLoading() },
                source = { flowOf { rankDirectory(unranked) } }
            ).map { ranking ->
                // Loading/error cannot erase the readable directory or invent selectable policy.
                ExtendedLoadingState.Loaded(ranking.dataOrNull ?: unranked)
            }
        }
    }
}
