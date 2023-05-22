package io.novafoundation.nova.feature_wallet_api.domain.implementations

import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.state.SelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chainAsset
import io.novafoundation.nova.runtime.state.selectedAssetFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

class SharedStateTokenUseCase(
    private val tokenRepository: TokenRepository,
    private val sharedState: SelectedAssetOptionSharedState<*>,
) : TokenUseCase {

    override suspend fun currentToken(): Token {
        val chainAsset = sharedState.chainAsset()

        return tokenRepository.getToken(chainAsset)
    }

    override fun currentTokenFlow(): Flow<Token> {
        return sharedState.selectedAssetFlow().flatMapLatest { chainAsset ->
            tokenRepository.observeToken(chainAsset)
        }
    }
}
