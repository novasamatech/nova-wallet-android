package io.novafoundation.nova.feature_assets.domain.tokens.add

import io.novafoundation.nova.runtime.ethereum.contract.base.querySingle
import io.novafoundation.nova.runtime.ethereum.contract.erc20.Erc20Queries
import io.novafoundation.nova.runtime.ethereum.contract.erc20.Erc20Standard
import io.novafoundation.nova.runtime.ext.defaultComparator
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.ethereumApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface AddTokensInteractor {

    fun availableChainsToAddTokenFlow(): Flow<List<Chain>>

    suspend fun retrieveContractMetadata(
        chainId: ChainId,
        contractAddress: String
    ): Erc20ContractMetadata?
}

class RealAddTokensInteractor(
    private val chainRegistry: ChainRegistry,
    private val erc20Standard: Erc20Standard,
) : AddTokensInteractor {

    override fun availableChainsToAddTokenFlow(): Flow<List<Chain>> {
        return chainRegistry.currentChains.map { chains ->
            chains.filter { it.isEthereumBased }
                .sortedWith(Chain.defaultComparator())
        }
    }

    override suspend fun retrieveContractMetadata(
        chainId: ChainId,
        contractAddress: String,
    ): Erc20ContractMetadata? {
        return runCatching {
            queryErc20Contract(chainId, contractAddress) {
                Erc20ContractMetadata(
                    decimals = executeOrNull { decimals().toInt() },
                    symbol = executeOrNull { symbol() }
                )
            }
        }.getOrNull()
    }

    private suspend fun <R> queryErc20Contract(
        chainId: ChainId,
        contractAddress: String,
        query: suspend Erc20Queries.() -> R
    ): R {
        val web3Api = chainRegistry.ethereumApi(chainId)
        val erc20Queries = erc20Standard.querySingle(contractAddress, web3Api)

        return query(erc20Queries)
    }

    private suspend fun <R> executeOrNull(action: suspend () -> R): R? = runCatching{ action() }.getOrNull()
}
