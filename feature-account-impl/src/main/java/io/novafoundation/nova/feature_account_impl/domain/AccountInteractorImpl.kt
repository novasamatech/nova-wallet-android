package io.novafoundation.nova.feature_account_impl.domain

import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.core.model.Language
import io.novafoundation.nova.core.model.Node
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.Account
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountOrdering
import io.novafoundation.nova.feature_account_api.domain.model.PreferredCryptoType
import io.novafoundation.nova.feature_account_impl.domain.errors.NodeAlreadyExistsException
import io.novafoundation.nova.feature_account_impl.domain.errors.UnsupportedNetworkException
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.encrypt.mnemonic.Mnemonic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AccountInteractorImpl(
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
) : AccountInteractor {

    override suspend fun generateMnemonic(): Mnemonic {
        return accountRepository.generateMnemonic()
    }

    override fun getCryptoTypes(): List<CryptoType> {
        return accountRepository.getEncryptionTypes()
    }

    override suspend fun getPreferredCryptoType(chainId: ChainId?): PreferredCryptoType = withContext(Dispatchers.Default) {
        if (chainId != null && chainRegistry.getChain(chainId).isEthereumBased) {
            PreferredCryptoType(CryptoType.ECDSA, frozen = true)
        } else {
            PreferredCryptoType(CryptoType.SR25519, frozen = false)
        }
    }

    override suspend fun isCodeSet(): Boolean {
        return accountRepository.isCodeSet()
    }

    override suspend fun savePin(code: String) {
        return accountRepository.savePinCode(code)
    }

    override suspend fun isPinCorrect(code: String): Boolean {
        val pinCode = accountRepository.getPinCode()

        return pinCode == code
    }

    override suspend fun isBiometricEnabled(): Boolean {
        return accountRepository.isBiometricEnabled()
    }

    override suspend fun setBiometricOn() {
        return accountRepository.setBiometricOn()
    }

    override suspend fun setBiometricOff() {
        return accountRepository.setBiometricOff()
    }

    override suspend fun getMetaAccount(metaId: Long): MetaAccount {
        return accountRepository.getMetaAccount(metaId)
    }

    override fun lightMetaAccountsFlow(): Flow<List<LightMetaAccount>> {
        return accountRepository.lightMetaAccountsFlow()
    }

    override fun selectedMetaAccountFlow(): Flow<MetaAccount> {
        return accountRepository.selectedMetaAccountFlow()
    }

    override suspend fun selectMetaAccount(metaId: Long) {
        accountRepository.selectMetaAccount(metaId)
    }

    override suspend fun deleteAccount(metaId: Long) = withContext(Dispatchers.Default) {
        accountRepository.deleteAccount(metaId)
    }

    override suspend fun updateMetaAccountPositions(idsInNewOrder: List<Long>) = with(Dispatchers.Default) {
        val ordering = idsInNewOrder.mapIndexed { index, id ->
            MetaAccountOrdering(id, index)
        }

        accountRepository.updateAccountsOrdering(ordering)
    }

    override fun nodesFlow(): Flow<List<Node>> {
        return accountRepository.nodesFlow()
    }

    override suspend fun getNode(nodeId: Int): Node {
        return accountRepository.getNode(nodeId)
    }

    override fun getLanguages(): List<Language> {
        return accountRepository.getLanguages()
    }

    override suspend fun getSelectedLanguage(): Language {
        return accountRepository.selectedLanguage()
    }

    override suspend fun changeSelectedLanguage(language: Language) {
        return accountRepository.changeLanguage(language)
    }

    override suspend fun addNode(nodeName: String, nodeHost: String): Result<Unit> {
        return ensureUniqueNode(nodeHost) {
            val networkType = getNetworkTypeByNodeHost(nodeHost)

            accountRepository.addNode(nodeName, nodeHost, networkType)
        }
    }

    override suspend fun updateNode(nodeId: Int, newName: String, newHost: String): Result<Unit> {
        return ensureUniqueNode(newHost) {
            val networkType = getNetworkTypeByNodeHost(newHost)

            accountRepository.updateNode(nodeId, newName, newHost, networkType)
        }
    }

    private suspend fun ensureUniqueNode(nodeHost: String, action: suspend () -> Unit): Result<Unit> {
        val nodeExists = accountRepository.checkNodeExists(nodeHost)

        return runCatching {
            if (nodeExists) {
                throw NodeAlreadyExistsException()
            } else {
                action()
            }
        }
    }

    /**
     * @throws UnsupportedNetworkException, if node network is not supported
     * @throws NovaException - in case of network issues
     */
    private suspend fun getNetworkTypeByNodeHost(nodeHost: String): Node.NetworkType {
        val networkName = accountRepository.getNetworkName(nodeHost)

        val supportedNetworks = Node.NetworkType.values()
        val networkType = supportedNetworks.firstOrNull { networkName == it.readableName }

        return networkType ?: throw UnsupportedNetworkException()
    }

    override suspend fun getAccountsByNetworkTypeWithSelectedNode(networkType: Node.NetworkType): Pair<List<Account>, Node> {
        val accounts = accountRepository.getAccountsByNetworkType(networkType)
        val node = accountRepository.getSelectedNodeOrDefault()
        return Pair(accounts, node)
    }

    override suspend fun selectNodeAndAccount(nodeId: Int, accountAddress: String) {
        val account = accountRepository.getAccount(accountAddress)
        val node = accountRepository.getNode(nodeId)

        accountRepository.selectAccount(account, newNode = node)
    }

    override suspend fun selectNode(nodeId: Int) {
        val node = accountRepository.getNode(nodeId)

        accountRepository.selectNode(node)
    }

    override suspend fun deleteNode(nodeId: Int) {
        return accountRepository.deleteNode(nodeId)
    }
}
