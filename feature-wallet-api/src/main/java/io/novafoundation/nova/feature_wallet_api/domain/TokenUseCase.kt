package io.novafoundation.nova.feature_wallet_api.domain

import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface TokenUseCase {

    suspend fun currentToken(): Token

    fun currentTokenFlow(): Flow<Token>

    suspend fun getToken(chainAssetId: FullChainAssetId): Token
}

fun TokenUseCase.currentAssetFlow(): Flow<Chain.Asset> {
    return currentTokenFlow().map { it.configuration }
}
