package io.novafoundation.nova.app.root.presentation.requestBusHandler

import kotlinx.coroutines.flow.Flow

interface RequestBusHandler {
    fun observe(): Flow<*>
}
