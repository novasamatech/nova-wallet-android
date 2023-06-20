package io.novafoundation.nova.feature_account_impl.presentation.common.sign.notSupported

import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.feature_account_impl.presentation.common.sign.notSupported.SigningNotSupportedPresentable.Payload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

interface SigningNotSupportedPresentable {

    class Payload(
        @DrawableRes val iconRes: Int,
        val message: String
    )

    suspend fun presentSigningNotSupported(payload: Payload)
}

class RealSigningNotSupportedPresentable(
    private val contextManager: ContextManager,
) : SigningNotSupportedPresentable {

    override suspend fun presentSigningNotSupported(payload: Payload): Unit = withContext(Dispatchers.Main) {
        suspendCoroutine {
            AcknowledgeSigningNotSupportedBottomSheet(
                context = contextManager.getActivity()!!,
                onConfirm = { it.resume(Unit) },
                payload = payload
            ).show()
        }
    }
}
