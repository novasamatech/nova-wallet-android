package io.novafoundation.nova.feature_account_impl.data.repository

import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.secrets.v2.getAccountSecrets
import io.novafoundation.nova.common.data.secrets.v2.seed
import io.novafoundation.nova.common.resources.LanguagesHolder
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.common.utils.networkType
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.core.model.Language
import io.novafoundation.nova.core.model.Network
import io.novafoundation.nova.core.model.Node
import io.novafoundation.nova.core_db.dao.AccountDao
import io.novafoundation.nova.core_db.dao.NodeDao
import io.novafoundation.nova.core_db.model.AccountLocal
import io.novafoundation.nova.core_db.model.NodeLocal
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus.Event
import io.novafoundation.nova.feature_account_api.data.secrets.keypair
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.Account
import io.novafoundation.nova.feature_account_api.domain.model.AuthType
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountAssetBalance
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountOrdering
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_account_api.domain.model.multiChainEncryptionIn
import io.novafoundation.nova.feature_account_api.domain.model.requireAddressIn
import io.novafoundation.nova.feature_account_impl.data.mappers.mapNodeLocalToNode
import io.novafoundation.nova.feature_account_impl.data.network.blockchain.AccountSubstrateSource
import io.novafoundation.nova.feature_account_impl.data.repository.datasource.AccountDataSource
import io.novafoundation.nova.feature_account_impl.data.repository.datasource.getMetaAccountTypeOrThrow
import io.novafoundation.nova.runtime.ext.genesisHash
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.encrypt.json.JsonSeedEncoder
import io.novasama.substrate_sdk_android.encrypt.mnemonic.Mnemonic
import io.novasama.substrate_sdk_android.encrypt.mnemonic.MnemonicCreator
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AccountRepositoryImpl(
    private val accountDataSource: AccountDataSource,
    private val accountDao: AccountDao,
    private val nodeDao: NodeDao,
    private val jsonSeedEncoder: JsonSeedEncoder,
    private val languagesHolder: LanguagesHolder,
    private val accountSubstrateSource: AccountSubstrateSource,
    private val secretStoreV2: SecretStoreV2,
    private val metaAccountChangesEventBus: MetaAccountChangesEventBus
) : AccountRepository {

    override fun getEncryptionTypes(): List<CryptoType> {
        return CryptoType.values().toList()
    }

    override suspend fun getNode(nodeId: Int): Node {
        return withContext(Dispatchers.IO) {
            val node = nodeDao.getNodeById(nodeId)

            mapNodeLocalToNode(node)
        }
    }

    override suspend fun getSelectedNodeOrDefault(): Node {
        return accountDataSource.getSelectedNode() ?: mapNodeLocalToNode(nodeDao.getFirstNode())
    }

    override suspend fun selectNode(node: Node) {
        accountDataSource.saveSelectedNode(node)
    }

    override suspend fun getDefaultNode(networkType: Node.NetworkType): Node {
        return mapNodeLocalToNode(nodeDao.getDefaultNodeFor(networkType.ordinal))
    }

    override suspend fun selectAccount(account: Account, newNode: Node?) {
        accountDataSource.saveSelectedAccount(account)

        when {
            newNode != null -> {
                require(account.network.type == newNode.networkType) {
                    "Account network type is not the same as chosen node type"
                }

                selectNode(newNode)
            }

            account.network.type != accountDataSource.getSelectedNode()?.networkType -> {
                val defaultNode = getDefaultNode(account.address.networkType())

                selectNode(defaultNode)
            }
        }
    }

    override suspend fun getSelectedMetaAccount(): MetaAccount {
        return accountDataSource.getSelectedMetaAccount()
    }

    override suspend fun getMetaAccount(metaId: Long): MetaAccount {
        return accountDataSource.getMetaAccount(metaId)
    }

    override fun metaAccountFlow(metaId: Long): Flow<MetaAccount> {
        return accountDataSource.metaAccountFlow(metaId)
    }

    override fun selectedMetaAccountFlow(): Flow<MetaAccount> {
        return accountDataSource.selectedMetaAccountFlow()
    }

    override suspend fun findMetaAccount(accountId: ByteArray, chainId: String): MetaAccount? {
        return accountDataSource.findMetaAccount(accountId, chainId)
    }

    override suspend fun accountNameFor(accountId: AccountId, chainId: String): String? {
        return accountDataSource.accountNameFor(accountId, chainId)
    }

    override suspend fun activeMetaAccounts(): List<MetaAccount> {
        return accountDataSource.activeMetaAccounts()
    }

    override suspend fun allLightMetaAccounts(): List<LightMetaAccount> {
        return accountDataSource.allLightMetaAccounts()
    }

    override suspend fun hasActiveMetaAccounts(): Boolean {
        return accountDataSource.hasActiveMetaAccounts()
    }

    override fun allMetaAccountsFlow(): Flow<List<MetaAccount>> {
        return accountDataSource.allMetaAccountsFlow()
    }

    override fun activeMetaAccountsFlow(): Flow<List<MetaAccount>> {
        return accountDataSource.activeMetaAccountsFlow()
    }

    override fun metaAccountBalancesFlow(): Flow<List<MetaAccountAssetBalance>> {
        return accountDataSource.metaAccountsWithBalancesFlow()
    }

    override fun metaAccountBalancesFlow(metaId: Long): Flow<List<MetaAccountAssetBalance>> {
        return accountDataSource.metaAccountBalancesFlow(metaId)
    }

    override suspend fun selectMetaAccount(metaId: Long) {
        return accountDataSource.selectMetaAccount(metaId)
    }

    override suspend fun updateMetaAccountName(metaId: Long, newName: String) = withContext(Dispatchers.Default) {
        accountDataSource.updateMetaAccountName(metaId, newName)

        val metaAccountType = requireNotNull(accountDataSource.getMetaAccountType(metaId))
        val event = Event.AccountNameChanged(metaId, metaAccountType)

        metaAccountChangesEventBus.notify(event, source = null)
    }

    override suspend fun isAccountSelected(): Boolean {
        return accountDataSource.anyAccountSelected()
    }

    override suspend fun deleteAccount(metaId: Long) = withContext(Dispatchers.Default) {
        val metaAccountType = accountDataSource.getMetaAccountTypeOrThrow(metaId)

        accountDataSource.deleteMetaAccount(metaId)

        metaAccountChangesEventBus.notify(Event.AccountRemoved(metaId, metaAccountType), source = null)
    }

    override suspend fun getAccounts(): List<Account> {
        return accountDao.getAccounts()
            .map { mapAccountLocalToAccount(it) }
    }

    override suspend fun getAccount(address: String): Account {
        val account = accountDao.getAccount(address) ?: throw NoSuchElementException("No account found for address $address")
        return mapAccountLocalToAccount(account)
    }

    override suspend fun getAccountOrNull(address: String): Account? {
        return accountDao.getAccount(address)?.let { mapAccountLocalToAccount(it) }
    }

    override suspend fun getMyAccounts(query: String, chainId: String): Set<Account> {
//        return withContext(Dispatchers.Default) {
//            accountDao.getAccounts(query, networkType)
//                .map { mapAccountLocalToAccount(it) }
//                .toSet()
//        }

        return emptySet() // TODO wallet
    }

    override suspend fun isCodeSet(): Boolean {
        return accountDataSource.getPinCode() != null
    }

    override suspend fun savePinCode(code: String) {
        return accountDataSource.savePinCode(code)
    }

    override suspend fun getPinCode(): String? {
        return accountDataSource.getPinCode()
    }

    override suspend fun generateMnemonic(): Mnemonic {
        return MnemonicCreator.randomMnemonic(Mnemonic.Length.TWELVE)
    }

    override fun isBiometricEnabledFlow(): Flow<Boolean> {
        return accountDataSource.getAuthTypeFlow().map { it == AuthType.BIOMETRY }
    }

    override fun isBiometricEnabled(): Boolean {
        return accountDataSource.getAuthType() == AuthType.BIOMETRY
    }

    override fun setBiometricOn() {
        return accountDataSource.saveAuthType(AuthType.BIOMETRY)
    }

    override fun setBiometricOff() {
        return accountDataSource.saveAuthType(AuthType.PINCODE)
    }

    override suspend fun updateAccountsOrdering(accountOrdering: List<MetaAccountOrdering>) {
        return accountDataSource.updateAccountPositions(accountOrdering)
    }

    override suspend fun generateRestoreJson(
        metaAccount: MetaAccount,
        chain: Chain,
        password: String,
    ): String {
        return withContext(Dispatchers.Default) {
            val accountId = metaAccount.accountIdIn(chain)!!
            val address = metaAccount.addressIn(chain)!!

            val secrets = secretStoreV2.getAccountSecrets(metaAccount.id, accountId)

            jsonSeedEncoder.generate(
                keypair = secrets.keypair(chain),
                seed = secrets.seed(),
                password = password,
                name = metaAccount.name,
                multiChainEncryption = metaAccount.multiChainEncryptionIn(chain)!!,
                genesisHash = chain.genesisHash.orEmpty(),
                address = address
            )
        }
    }

    override suspend fun isAccountExists(accountId: AccountId, chainId: String): Boolean {
        return accountDataSource.accountExists(accountId, chainId)
    }

    override suspend fun removeDeactivatedMetaAccounts() {
        accountDataSource.removeDeactivatedMetaAccounts()
    }

    override suspend fun getActiveMetaAccounts(): List<MetaAccount> {
        return accountDataSource.getActiveMetaAccounts()
    }

    override suspend fun getActiveMetaAccountsQuantity(): Int {
        return accountDataSource.getActiveMetaAccountsQuantity()
    }

    override fun nodesFlow(): Flow<List<Node>> {
        return nodeDao.nodesFlow()
            .mapList { mapNodeLocalToNode(it) }
            .filter { it.isNotEmpty() }
            .flowOn(Dispatchers.Default)
    }

    override fun getLanguages(): List<Language> {
        return languagesHolder.getLanguages()
    }

    override suspend fun selectedLanguage(): Language {
        return accountDataSource.getSelectedLanguage()
    }

    override suspend fun changeLanguage(language: Language) {
        return accountDataSource.changeSelectedLanguage(language)
    }

    override suspend fun addNode(nodeName: String, nodeHost: String, networkType: Node.NetworkType) {
        val nodeLocal = NodeLocal(nodeName, nodeHost, networkType.ordinal, false)
        nodeDao.insert(nodeLocal)
    }

    override suspend fun updateNode(nodeId: Int, newName: String, newHost: String, networkType: Node.NetworkType) {
        nodeDao.updateNode(nodeId, newName, newHost, networkType.ordinal)
    }

    override suspend fun checkNodeExists(nodeHost: String): Boolean {
        return nodeDao.checkNodeExists(nodeHost)
    }

    override suspend fun getNetworkName(nodeHost: String): String {
        return accountSubstrateSource.getNodeNetworkType(nodeHost)
    }

    override suspend fun getAccountsByNetworkType(networkType: Node.NetworkType): List<Account> {
        val accounts = accountDao.getAccountsByNetworkType(networkType.ordinal)

        return withContext(Dispatchers.Default) {
            accounts.map { mapAccountLocalToAccount(it) }
        }
    }

    override suspend fun deleteNode(nodeId: Int) {
        return nodeDao.deleteNode(nodeId)
    }

    override suspend fun createQrAccountContent(chain: Chain, account: MetaAccount): String {
        return account.requireAddressIn(chain)
    }

    private fun mapAccountLocalToAccount(accountLocal: AccountLocal): Account {
        val network = getNetworkForType(accountLocal.networkType)

        return with(accountLocal) {
            Account(
                address = address,
                name = username,
                accountIdHex = publicKey,
                cryptoType = CryptoType.values()[accountLocal.cryptoType],
                network = network,
                position = position
            )
        }
    }

    private fun getNetworkForType(networkType: Node.NetworkType): Network {
        return Network(networkType)
    }
}
