package io.novafoundation.nova.feature_account_impl.presentation.watchOnly.sign

import io.novafoundation.nova.common.resources.ContextManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface WatchOnlySigningPresenter {

    suspend fun presentSigningNotPossible()
}

class RealWatchOnlySigningPresenter(
    private val contextManager: ContextManager,
) : WatchOnlySigningPresenter {

    override suspend fun presentSigningNotPossible(): Unit = withContext(Dispatchers.Main) {
        suspendCoroutine {
            WatchOnlySignBottomSheet(
                context = contextManager.getActivity()!!,
                onSuccess = { it.resume(Unit) }
            ).show()
        }
    }
}
