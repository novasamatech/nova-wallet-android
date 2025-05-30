package io.novafoundation.nova.feature_deep_linking.presentation.handling

import android.net.Uri
import kotlinx.coroutines.flow.Flow

interface DeepLinkHandler {

    val callbackFlow: Flow<CallbackEvent>

    suspend fun matches(data: Uri): Boolean

    suspend fun handleDeepLink(data: Uri): Result<Unit>
}

sealed interface CallbackEvent {
    data class Message(val message: String) : CallbackEvent
}
