package io.novafoundation.nova.feature_account_api.domain.interfaces

import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.core.model.Language
import io.novafoundation.nova.core.model.Node
import io.novafoundation.nova.feature_account_api.domain.model.Account
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.PreferredCryptoType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.encrypt.mnemonic.Mnemonic
import kotlinx.coroutines.flow.Flow

interface AccountInteractor {

    suspend fun getActiveMetaAccounts(): List<MetaAccount>

    suspend fun generateMnemonic(): Mnemonic

    fun getCryptoTypes(): List<CryptoType>

    suspend fun getPreferredCryptoType(chainId: ChainId? = null): PreferredCryptoType

    suspend fun isCodeSet(): Boolean

    suspend fun savePin(code: String)

    suspend fun isPinCorrect(code: String): Boolean

    suspend fun getMetaAccount(metaId: Long): MetaAccount

    suspend fun selectMetaAccount(metaId: Long)

    suspend fun deleteAccount(metaId: Long): Boolean

    suspend fun updateMetaAccountPositions(idsInNewOrder: List<Long>)

    fun nodesFlow(): Flow<List<Node>>

    suspend fun getNode(nodeId: Int): Node

    fun getLanguages(): List<Language>

    suspend fun getSelectedLanguage(): Language

    suspend fun changeSelectedLanguage(language: Language)

    suspend fun addNode(nodeName: String, nodeHost: String): Result<Unit>

    suspend fun updateNode(nodeId: Int, newName: String, newHost: String): Result<Unit>

    suspend fun getAccountsByNetworkTypeWithSelectedNode(networkType: Node.NetworkType): Pair<List<Account>, Node>

    suspend fun selectNodeAndAccount(nodeId: Int, accountAddress: String)

    suspend fun selectNode(nodeId: Int)

    suspend fun deleteNode(nodeId: Int)

    suspend fun getChainAddress(metaId: Long, chainId: ChainId): String?

    suspend fun removeDeactivatedMetaAccounts()

    suspend fun switchToNotDeactivatedAccountIfNeeded()
}
