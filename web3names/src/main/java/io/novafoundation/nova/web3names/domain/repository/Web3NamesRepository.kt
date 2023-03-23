package io.novafoundation.nova.web3names.domain.repository

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.web3names.domain.models.Web3NameAccount

interface Web3NamesRepository {

    suspend fun queryWeb3NameAccount(web3Name: String, chain: Chain, chainAsset: Chain.Asset): Result<List<Web3NameAccount>>

    fun isValidWeb3NameAccount(web3NameAccount: Web3NameAccount): Boolean
}
