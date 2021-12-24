package io.novafoundation.nova.feature_wallet_api.domain.implementations

import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.chainAsset
import io.novafoundation.nova.runtime.state.selectedAssetFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

class SharedStateTokenUseCase(
    private val tokenRepository: TokenRepository,
    private val sharedState: SingleAssetSharedState,
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

class FixedTokenUseCase(
    private val tokenRepository: TokenRepository,
    private val chainId: ChainId,
    private val chainRegistry: ChainRegistry,
    private val chainAssetId: Int,
) : TokenUseCase {

    override suspend fun currentToken(): Token {
        val chainAsset = chainRegistry.asset(chainId, chainAssetId)

        return tokenRepository.getToken(chainAsset)
    }

    override fun currentTokenFlow(): Flow<Token> {
        return flow {
            val chainAsset = chainRegistry.asset(chainId, chainAssetId)

            emitAll(tokenRepository.observeToken(chainAsset))
        }
    }
}

class GenesisHashUtilityTokenUseCase(
    private val genesisHash: String,
    private val chainRegistry: ChainRegistry,
    private val tokenRepository: TokenRepository,
) : TokenUseCase {

    override suspend fun currentToken(): Token {
        return tokenRepository.getToken(getChainAsset())
    }

    override fun currentTokenFlow(): Flow<Token> {
        return flow {
            emitAll(tokenRepository.observeToken(getChainAsset()))
        }
    }

    private suspend fun getChainAsset(): Chain.Asset {
        return chainRegistry.getChain(genesisHash).utilityAsset
    }
}
