package jp.co.soramitsu.feature_account_api.domain.interfaces

import jp.co.soramitsu.core.model.CryptoType
import jp.co.soramitsu.core.model.Language
import jp.co.soramitsu.core.model.Node
import jp.co.soramitsu.feature_account_api.domain.model.Account
import jp.co.soramitsu.feature_account_api.domain.model.LightMetaAccount
import jp.co.soramitsu.feature_account_api.domain.model.MetaAccount
import jp.co.soramitsu.feature_account_api.domain.model.PreferredCryptoType
import jp.co.soramitsu.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow

interface AccountInteractor {

    suspend fun generateMnemonic(): List<String>

    fun getCryptoTypes(): List<CryptoType>

    suspend fun getPreferredCryptoType(chainId: ChainId? = null): PreferredCryptoType

    suspend fun isCodeSet(): Boolean

    suspend fun savePin(code: String)

    suspend fun isPinCorrect(code: String): Boolean

    suspend fun isBiometricEnabled(): Boolean

    suspend fun setBiometricOn()

    suspend fun setBiometricOff()

    suspend fun getMetaAccount(metaId: Long): MetaAccount

    fun lightMetaAccountsFlow(): Flow<List<LightMetaAccount>>

    fun selectedMetaAccountFlow(): Flow<MetaAccount>

    suspend fun selectMetaAccount(metaId: Long)

    suspend fun deleteAccount(metaId: Long)

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
}
