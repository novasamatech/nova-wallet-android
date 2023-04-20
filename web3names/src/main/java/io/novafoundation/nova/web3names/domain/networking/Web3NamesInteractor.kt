package io.novafoundation.nova.web3names.domain.networking

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.web3names.data.repository.Web3NamesRepository
import io.novafoundation.nova.web3names.domain.exceptions.ParseWeb3NameException
import io.novafoundation.nova.web3names.domain.models.Web3NameAccount

interface Web3NamesInteractor {

    fun isValidWeb3Name(raw: String): Boolean

    suspend fun queryAccountsByWeb3Name(w3nIdentifier: String, chain: Chain, chainAsset: Chain.Asset): List<Web3NameAccount>

    fun removePrefix(w3nIdentifier: String): String
}

class RealWeb3NamesInteractor(
    private val web3NamesRepository: Web3NamesRepository
) : Web3NamesInteractor {

    override fun isValidWeb3Name(raw: String): Boolean {
        return parseToWeb3Name(raw).isSuccess
    }

    override suspend fun queryAccountsByWeb3Name(w3nIdentifier: String, chain: Chain, chainAsset: Chain.Asset): List<Web3NameAccount> {
        require(isValidWeb3Name(w3nIdentifier))

        val web3NameNoPrefix = parseToWeb3Name(w3nIdentifier).getOrThrow()

        return web3NamesRepository.queryWeb3NameAccount(web3NameNoPrefix, chain, chainAsset)
    }

    override fun removePrefix(w3nIdentifier: String): String {
        require(isValidWeb3Name(w3nIdentifier))

        return parseToWeb3Name(w3nIdentifier).getOrThrow()
    }

    private fun parseToWeb3Name(raw: String): Result<String> {
        return runCatching {
            val (web3NameKey, web3NameValue) = raw.split(":", limit = 2)

            if (web3NameKey.trim() == "w3n") {
                web3NameValue.trim()
            } else {
                throw ParseWeb3NameException()
            }
        }
    }
}
