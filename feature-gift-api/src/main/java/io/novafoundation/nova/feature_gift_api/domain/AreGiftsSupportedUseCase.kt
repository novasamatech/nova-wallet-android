package io.novafoundation.nova.feature_gift_api.domain

import kotlinx.coroutines.flow.Flow

interface AreGiftsSupportedUseCase {
    suspend fun areGiftsSupported(): Boolean

    fun areGiftsSupportedFlow(): Flow<Boolean>
}
