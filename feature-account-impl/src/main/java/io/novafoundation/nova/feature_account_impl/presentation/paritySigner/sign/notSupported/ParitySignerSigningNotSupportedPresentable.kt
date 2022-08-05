package io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.notSupported

import io.novafoundation.nova.common.resources.ContextManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface ParitySignerSigningNotSupportedPresentable {

    suspend fun presentSigningNotSupported()
}

class RealParitySignerSigningNotSupportedPresentable(
    private val contextManager: ContextManager,
) : ParitySignerSigningNotSupportedPresentable {

    override suspend fun presentSigningNotSupported(): Unit = withContext(Dispatchers.Main) {
        suspendCoroutine {
            AcknowledgeSigningNotSupportedBottomSheet(
                context = contextManager.getActivity()!!,
                onConfirm = { it.resume(Unit) }
            ).show()
        }
    }
}

