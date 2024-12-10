package io.novafoundation.nova.feature_wallet_api.domain.interfaces

import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.flow.Flow

interface TokenRepository {

    /**
     * Observes tokens for given [chainAssets] associated by [FullChainAssetId].
     * Emitted map will contain keys for all supplied [chainAssets], even if some prices are currently unknown
     */
    suspend fun observeTokens(chainAssets: List<Chain.Asset>): Flow<Map<FullChainAssetId, Token>>

    suspend fun getTokens(chainAsset: List<Chain.Asset>): Map<FullChainAssetId, Token>

    suspend fun getToken(chainAsset: Chain.Asset): Token

    suspend fun getTokenOrNull(chainAsset: Chain.Asset): Token?

    fun observeToken(chainAsset: Chain.Asset): Flow<Token>
}
