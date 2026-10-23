package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import io.novafoundation.nova.common.address.AccountIdKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

private const val FALLBACK_PREFIX = 6

private const val FALLBACK_SUFFIX = 7

/**
 * Directory names of the validators, null until a lookup succeeds. As on iOS a failed lookup is asked again on the
 * next of [retries], and a resolved one is kept while the validators stay the same.
 */
internal fun Flow<Set<AccountIdKey>>.resolvedValidatorNames(
    retries: Flow<Unit>,
    onFailure: (Exception) -> Unit = {},
    lookup: suspend (Set<AccountIdKey>) -> Map<AccountIdKey, String>
): Flow<Map<AccountIdKey, String>?> = distinctUntilChanged().flatMapLatest { hotkeys ->
    flow {
        emit(null)
        while (true) {
            val names = try {
                lookup(hotkeys)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onFailure(e)
                null
            }
            if (names != null) return@flow emit(names)
            retries.first()
        }
    }
}

/** iOS `mediumTruncated`: the address a validator without a directory name reads as. */
internal fun String.bittensorValidatorFallbackName(): String {
    return if (length > FALLBACK_PREFIX + FALLBACK_SUFFIX) take(FALLBACK_PREFIX) + "..." + takeLast(FALLBACK_SUFFIX) else this
}
