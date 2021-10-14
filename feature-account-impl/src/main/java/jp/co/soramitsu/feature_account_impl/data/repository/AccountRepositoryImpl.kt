package jp.co.soramitsu.feature_account_impl.data.repository

import jp.co.soramitsu.common.data.mappers.mapCryptoTypeToEncryption
import jp.co.soramitsu.common.resources.LanguagesHolder
import jp.co.soramitsu.common.utils.mapList
import jp.co.soramitsu.common.utils.networkType
import jp.co.soramitsu.core.model.CryptoType
import jp.co.soramitsu.core.model.JsonFormer
import jp.co.soramitsu.core.model.Language
import jp.co.soramitsu.core.model.Network
import jp.co.soramitsu.core.model.Node
import jp.co.soramitsu.core.model.SecuritySource
import jp.co.soramitsu.core.model.WithJson
import jp.co.soramitsu.core.model.chainId
import jp.co.soramitsu.core_db.dao.AccountDao
import jp.co.soramitsu.core_db.dao.NodeDao
import jp.co.soramitsu.core_db.model.AccountLocal
import jp.co.soramitsu.core_db.model.NodeLocal
import jp.co.soramitsu.fearless_utils.encrypt.json.JsonSeedEncoder
import jp.co.soramitsu.fearless_utils.encrypt.mnemonic.Mnemonic
import jp.co.soramitsu.fearless_utils.encrypt.mnemonic.MnemonicCreator
import jp.co.soramitsu.fearless_utils.encrypt.qr.QrSharing
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.feature_account_api.domain.interfaces.AccountRepository
import jp.co.soramitsu.feature_account_api.domain.model.Account
import jp.co.soramitsu.feature_account_api.domain.model.AuthType
import jp.co.soramitsu.feature_account_api.domain.model.LightMetaAccount
import jp.co.soramitsu.feature_account_api.domain.model.MetaAccount
import jp.co.soramitsu.feature_account_api.domain.model.MetaAccountOrdering
import jp.co.soramitsu.feature_account_api.domain.model.addressIn
import jp.co.soramitsu.feature_account_api.domain.model.publicKeyIn
import jp.co.soramitsu.feature_account_impl.data.mappers.mapNodeLocalToNode
import jp.co.soramitsu.feature_account_impl.data.network.blockchain.AccountSubstrateSource
import jp.co.soramitsu.feature_account_impl.data.repository.datasource.AccountDataSource
import jp.co.soramitsu.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
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

    // TODO remove
    override fun selectedAccountFlow(): Flow<Account> {
        return accountDataSource.selectedAccountMapping.map {
            it.getValue(Node.NetworkType.POLKADOT.chainId)!!
        }
    }

    // TODO remove
    override suspend fun getSelectedAccount(): Account {
        return getSelectedAccount(Node.NetworkType.POLKADOT.chainId)
    }

    override suspend fun getSelectedAccount(chainId: String): Account {
        return accountDataSource.selectedAccountMapping.first().getValue(chainId)!!
    }

    override suspend fun getSelectedMetaAccount(): MetaAccount {
        return accountDataSource.getSelectedMetaAccount()
    }

    override suspend fun getMetaAccount(metaId: Long): MetaAccount {
        return accountDataSource.getMetaAccount(metaId)
    }

    override fun selectedMetaAccountFlow(): Flow<MetaAccount> {
        return accountDataSource.selectedMetaAccountFlow()
    }

    override suspend fun findMetaAccount(accountId: ByteArray): MetaAccount? {
        return accountDataSource.findMetaAccount(accountId)
    }

    override suspend fun allMetaAccounts(): List<MetaAccount> {
        return accountDataSource.allMetaAccounts()
    }

    override fun lightMetaAccountsFlow(): Flow<List<LightMetaAccount>> {
        return accountDataSource.lightMetaAccountsFlow()
    }

    override suspend fun selectMetaAccount(metaId: Long) {
        return accountDataSource.selectMetaAccount(metaId)
    }

    override suspend fun updateMetaAccountName(metaId: Long, newName: String) {
        return accountDataSource.updateMetaAccountName(metaId, newName)
    }

    override suspend fun getPreferredCryptoType(): CryptoType {
        return accountDataSource.getPreferredCryptoType()
    }

    override suspend fun isAccountSelected(): Boolean {
        return accountDataSource.anyAccountSelected()
    }

    override suspend fun deleteAccount(metaId: Long) {
        accountDataSource.deleteMetaAccount(metaId)
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

    override suspend fun generateMnemonic(): List<String> {
        return withContext(Dispatchers.Default) {
            val generationResult = MnemonicCreator.randomMnemonic(Mnemonic.Length.TWELVE)

            generationResult.wordList
        }
    }

    override suspend fun isBiometricEnabled(): Boolean {
        return accountDataSource.getAuthType() == AuthType.BIOMETRY
    }

    override suspend fun setBiometricOn() {
        return accountDataSource.saveAuthType(AuthType.BIOMETRY)
    }

    override suspend fun setBiometricOff() {
        return accountDataSource.saveAuthType(AuthType.PINCODE)
    }

    override suspend fun updateAccountsOrdering(accountOrdering: List<MetaAccountOrdering>) {
        return accountDataSource.updateAccountPositions(accountOrdering)
    }

    override suspend fun getSecuritySource(accountAddress: String): SecuritySource {
        return accountDataSource.getSecuritySource(accountAddress)!!
    }

    override suspend fun generateRestoreJson(account: Account, password: String): String {
        return withContext(Dispatchers.Default) {
            val securitySource = getSecuritySource(account.address)
            require(securitySource is WithJson)

            val seed = (securitySource.jsonFormer() as? JsonFormer.Seed)?.seed

            val cryptoType = mapCryptoTypeToEncryption(account.cryptoType)
            val runtimeConfiguration = account.network.type.runtimeConfiguration

            jsonSeedEncoder.generate(
                keypair = securitySource.keypair,
                seed = seed,
                password = password,
                name = account.name.orEmpty(),
                encryptionType = cryptoType,
                genesisHash = runtimeConfiguration.genesisHash,
                addressByte = runtimeConfiguration.addressByte
            )
        }
    }

    override suspend fun isAccountExists(accountId: AccountId): Boolean {
        return accountDataSource.accountExists(accountId)
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
        val payload = QrSharing.Payload(
            address = account.addressIn(chain)!!,
            publicKey = account.publicKeyIn(chain)!!,
            name = account.name
        )

        return QrSharing.encode(payload)
    }

    private suspend fun mapAccountLocalToAccount(accountLocal: AccountLocal): Account {
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
