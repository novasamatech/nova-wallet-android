package io.novafoundation.nova.feature_wallet_api.domain.interfaces

import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

interface TokenRepository {

    suspend fun getToken(chainAsset: Chain.Asset): Token

    fun observeToken(chainAsset: Chain.Asset): Flow<Token>
}
