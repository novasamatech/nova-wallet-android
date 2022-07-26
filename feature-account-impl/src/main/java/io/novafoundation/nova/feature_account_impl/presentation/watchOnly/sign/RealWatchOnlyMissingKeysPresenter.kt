package io.novafoundation.nova.feature_account_impl.presentation.watchOnly.sign

import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.feature_account_api.presenatation.account.watchOnly.WatchOnlyMissingKeysPresenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class RealWatchOnlyMissingKeysPresenter(
    private val contextManager: ContextManager,
) : WatchOnlyMissingKeysPresenter {

    override suspend fun presentNoKeysFound(): Unit = withContext(Dispatchers.Main) {
        suspendCoroutine {
            WatchOnlySignBottomSheet(
                context = contextManager.getActivity()!!,
                onSuccess = { it.resume(Unit) }
            ).show()
        }
    }
}
