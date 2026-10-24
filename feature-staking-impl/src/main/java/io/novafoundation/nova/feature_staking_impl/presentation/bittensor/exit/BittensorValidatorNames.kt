package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import io.novafoundation.nova.common.address.AccountIdKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

/**
 * Directory names of the validators, null until a lookup succeeds. As on iOS a failed lookup is asked again on the
 * next of [retries], and a resolved one is kept while the validators stay the same. Without [retries] a failed lookup stays null.
 */
internal fun Flow<Set<AccountIdKey>>.resolvedValidatorNames(
    retries: Flow<Unit> = emptyFlow(),
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
            retries.firstOrNull() ?: return@flow
        }
    }
}
