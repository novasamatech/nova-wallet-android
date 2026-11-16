package io.novafoundation.nova.feature_account_api.domain.account

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take

class SelectedMetaAccountChangeObserver(
    private val accountRepository: AccountRepository
) {

    /**
     * Observes selection changes after the first emitted account. Updates to the same account are ignored.
     * The subscription and callback run in [scope] until it or the returned job is cancelled.
     */
    fun observe(scope: CoroutineScope, onChange: suspend (metaAccountId: Long) -> Unit): Job {
        return selectedMetaAccountIds()
            .drop(1)
            .onEach(onChange)
            .launchIn(scope)
    }

    /**
     * Reacts once to a selection different from [initialMetaAccountId], including the first emitted account.
     * Use this when the caller already belongs to a specific wallet, for example one passed in a payload.
     */
    fun observeOnce(initialMetaAccountId: Long, scope: CoroutineScope, onChange: suspend (metaAccountId: Long) -> Unit): Job {
        return selectedMetaAccountIds()
            .filter { it != initialMetaAccountId }
            .take(1)
            .onEach(onChange)
            .launchIn(scope)
    }

    private fun selectedMetaAccountIds(): Flow<Long> {
        return accountRepository.selectedMetaAccountFlow()
            .map { it.id }
            .distinctUntilChanged()
    }
}
