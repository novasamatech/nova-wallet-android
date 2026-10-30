package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.domain.ExtendedLoadingState

internal inline fun <T> bittensorLoadingValue(
    vararg dependencies: ExtendedLoadingState<*>,
    value: () -> T
): ExtendedLoadingState<T> {
    val failure = dependencies.filterIsInstance<ExtendedLoadingState.Error>().firstOrNull()
    if (failure != null) return failure
    if (dependencies.any { it is ExtendedLoadingState.Loading }) return ExtendedLoadingState.Loading

    return ExtendedLoadingState.Loaded(value())
}
