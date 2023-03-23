package io.novafoundation.nova.web3names.domain.networking

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.web3names.domain.exceptions.Web3NamesException
import io.novafoundation.nova.web3names.domain.models.Web3NameAccount
import io.novafoundation.nova.web3names.domain.repository.Web3NamesRepository

interface Web3NamesInteractor {

    fun isValidWeb3Name(raw: String): Boolean

    suspend fun queryAccountsByWeb3Name(web3Name: String, chain: Chain, chainAsset: Chain.Asset): Result<List<Web3NameAccount>>

    fun isValidWeb3NameAccount(web3NameAccount: Web3NameAccount): Boolean
}

class RealWeb3NamesInteractor(
    private val web3NamesRepository: Web3NamesRepository
) : Web3NamesInteractor {
    override fun isValidWeb3Name(raw: String): Boolean {
        return parseToWeb3Name(raw).isSuccess
    }

    override suspend fun queryAccountsByWeb3Name(web3Name: String, chain: Chain, chainAsset: Chain.Asset): Result<List<Web3NameAccount>> {
        require(isValidWeb3Name(web3Name))

        val web3NameNoPrefix = parseToWeb3Name(web3Name).getOrThrow()

        return web3NamesRepository.queryWeb3NameAccount(web3NameNoPrefix, chain, chainAsset)
    }

    override fun isValidWeb3NameAccount(web3NameAccount: Web3NameAccount): Boolean {
        return web3NamesRepository.isValidWeb3NameAccount(web3NameAccount)
    }

    private fun parseToWeb3Name(raw: String): Result<String> {
        return runCatching {
            val (web3NameKey, web3NameValue) = raw.split(":", limit = 2)

            if (web3NameKey.trim() == "w3n") {
                web3NameValue.trim()
            } else {
                throw Web3NamesException.ParseWeb3NameException()
            }
        }
    }
}
