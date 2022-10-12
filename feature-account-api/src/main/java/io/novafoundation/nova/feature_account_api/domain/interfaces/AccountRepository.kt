package io.novafoundation.nova.feature_account_api.domain.interfaces

import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.core.model.Language
import io.novafoundation.nova.core.model.Node
import io.novafoundation.nova.feature_account_api.domain.model.Account
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountAssetBalance
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountOrdering
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.encrypt.mnemonic.Mnemonic
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow

class AccountAlreadyExistsException : Exception()

interface AccountRepository {

    fun getEncryptionTypes(): List<CryptoType>

    suspend fun getNode(nodeId: Int): Node

    suspend fun getSelectedNodeOrDefault(): Node

    suspend fun selectNode(node: Node)

    suspend fun getDefaultNode(networkType: Node.NetworkType): Node

    suspend fun selectAccount(account: Account, newNode: Node? = null)

    suspend fun getSelectedAccount(chainId: String): Account

    suspend fun getSelectedMetaAccount(): MetaAccount

    suspend fun getMetaAccount(metaId: Long): MetaAccount

    fun selectedMetaAccountFlow(): Flow<MetaAccount>

    suspend fun findMetaAccount(accountId: ByteArray): MetaAccount?

    suspend fun accountNameFor(accountId: AccountId): String?

    suspend fun allMetaAccounts(): List<MetaAccount>

    fun allMetaAccountsFlow(): Flow<List<MetaAccount>>

    fun metaAccountBalancesFlow(): Flow<List<MetaAccountAssetBalance>>

    suspend fun selectMetaAccount(metaId: Long)

    suspend fun updateMetaAccountName(metaId: Long, newName: String)

    suspend fun isAccountSelected(): Boolean

    suspend fun deleteAccount(metaId: Long)

    suspend fun getAccounts(): List<Account>

    suspend fun getAccount(address: String): Account

    suspend fun getAccountOrNull(address: String): Account?

    suspend fun getMyAccounts(query: String, chainId: String): Set<Account>

    suspend fun isCodeSet(): Boolean

    suspend fun savePinCode(code: String)

    suspend fun getPinCode(): String?

    suspend fun generateMnemonic(): Mnemonic

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
     * @throws NovaException
     * @throws NovaException
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
