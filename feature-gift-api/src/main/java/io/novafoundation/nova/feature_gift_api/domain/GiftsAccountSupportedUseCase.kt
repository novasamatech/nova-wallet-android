package io.novafoundation.nova.feature_gift_api.domain

import kotlinx.coroutines.flow.Flow

enum class GiftsSupportedState {
    SUPPORTED,
    UNSUPPORTED_MULTISIG_ACCOUNTS
}

interface GiftsAccountSupportedUseCase {
    suspend fun supportedState(): GiftsSupportedState

    fun areGiftsSupportedFlow(): Flow<GiftsSupportedState>
}
