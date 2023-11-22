package io.novafoundation.nova.app.root.presentation.deepLinks

import android.net.Uri
import kotlinx.coroutines.flow.Flow

interface DeepLinkHandlerFactory {

    fun create(): DeepLinkHandler
}

interface DeepLinkHandler {

    val callbackFlow: Flow<CallbackEvent>

    suspend fun matches(data: Uri): Boolean

    suspend fun handleDeepLink(data: Uri)
}

sealed interface CallbackEvent {
    data class Message(val message: String) : CallbackEvent
}
