package jp.co.soramitsu.feature_account_api.domain.interfaces

import jp.co.soramitsu.core.model.CryptoType
import jp.co.soramitsu.core.model.Language
import jp.co.soramitsu.core.model.Node
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.feature_account_api.domain.model.Account
import jp.co.soramitsu.feature_account_api.domain.model.LightMetaAccount
import jp.co.soramitsu.feature_account_api.domain.model.MetaAccount
import jp.co.soramitsu.feature_account_api.domain.model.MetaAccountOrdering
import jp.co.soramitsu.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

class AccountAlreadyExistsException : Exception()

interface AccountRepository {

    fun getEncryptionTypes(): List<CryptoType>

    suspend fun getNode(nodeId: Int): Node

    suspend fun getSelectedNodeOrDefault(): Node

    suspend fun selectNode(node: Node)

    suspend fun getDefaultNode(networkType: Node.NetworkType): Node

    suspend fun selectAccount(account: Account, newNode: Node? = null)

    suspend fun getSelectedAccount(): Account

    suspend fun getSelectedAccount(chainId: String): Account
    suspend fun getSelectedMetaAccount(): MetaAccount
    suspend fun getMetaAccount(metaId: Long): MetaAccount
    fun selectedMetaAccountFlow(): Flow<MetaAccount>

    suspend fun findMetaAccount(accountId: ByteArray): MetaAccount?

    suspend fun allMetaAccounts(): List<MetaAccount>

    fun lightMetaAccountsFlow(): Flow<List<LightMetaAccount>>
    suspend fun selectMetaAccount(metaId: Long)

    suspend fun updateMetaAccountName(metaId: Long, newName: String)

    suspend fun getPreferredCryptoType(): CryptoType

    suspend fun isAccountSelected(): Boolean

    suspend fun deleteAccount(metaId: Long)

    suspend fun getAccounts(): List<Account>

    suspend fun getAccount(address: String): Account

    suspend fun getAccountOrNull(address: String): Account?

    suspend fun getMyAccounts(query: String, chainId: String): Set<Account>

    suspend fun isCodeSet(): Boolean

    suspend fun savePinCode(code: String)

    suspend fun getPinCode(): String?

    suspend fun generateMnemonic(): List<String>

    suspend fun isBiometricEnabled(): Boolean

    suspend fun setBiometricOn()

    suspend fun setBiometricOff()

    fun nodesFlow(): Flow<List<Node>>

    suspend fun updateAccountsOrdering(accountOrdering: List<MetaAccountOrdering>)

    fun getLanguages(): List<Language>

    suspend fun selectedLanguage(): Language

    suspend fun changeLanguage(language: Language)

    suspend fun addNode(nodeName: String, nodeHost: String, networkType: Node.NetworkType)

    suspend fun updateNode(nodeId: Int, newName: String, newHost: String, networkType: Node.NetworkType)

    suspend fun checkNodeExists(nodeHost: String): Boolean

    /**
     * @throws FearlessException
     */
    suspend fun getNetworkName(nodeHost: String): String

    suspend fun getAccountsByNetworkType(networkType: Node.NetworkType): List<Account>

    suspend fun deleteNode(nodeId: Int)

    suspend fun createQrAccountContent(chain: Chain, account: MetaAccount): String

    suspend fun generateRestoreJson(
        metaAccount: MetaAccount,
        chain: Chain,
        password: String
    ): String

    suspend fun isAccountExists(accountId: AccountId): Boolean
}
