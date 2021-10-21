package io.novafoundation.nova.feature_wallet_api.domain

import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import kotlinx.coroutines.flow.Flow

interface TokenUseCase {

    suspend fun currentToken(): Token

    fun currentTokenFlow(): Flow<Token>
}
