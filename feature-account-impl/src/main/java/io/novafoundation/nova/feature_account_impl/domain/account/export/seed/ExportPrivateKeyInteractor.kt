package io.novafoundation.nova.feature_account_impl.domain.account.export.seed

import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.secrets.v2.ethereumKeyPair
import io.novafoundation.nova.common.data.secrets.v2.ethereumKeypair
import io.novafoundation.nova.common.data.secrets.v2.privateKey
import io.novafoundation.nova.common.data.secrets.v2.seed
import io.novafoundation.nova.feature_account_api.data.secrets.getAccountSecrets
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.extensions.toHexString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExportPrivateKeyInteractor(
    private val accountRepository: AccountRepository,
    private val secretStoreV2: SecretStoreV2,
    private val chainRegistry: ChainRegistry,
) {

    suspend fun getAccountSeed(
        metaId: Long,
        chainId: ChainId,
    ): String = withContext(Dispatchers.Default) {
        val metaAccount = accountRepository.getMetaAccount(metaId)
        val chain = chainRegistry.getChain(chainId)

        val accountSecrets = secretStoreV2.getAccountSecrets(metaAccount, chain)

        accountSecrets.seed()?.toHexString(withPrefix = true)
            ?: error("No seed found for account ${metaAccount.name} in ${chain.name}")
    }

    suspend fun getEthereumPrivateKey(
        metaId: Long,
        chainId: ChainId,
    ): String = withContext(Dispatchers.Default) {
        val metaAccount = accountRepository.getMetaAccount(metaId)
        val chain = chainRegistry.getChain(chainId)

        require(chain.isEthereumBased) { "Chain ${chain.name} is not Ethereum-based" }

        val accountSecrets = secretStoreV2.getAccountSecrets(metaAccount, chain)

        accountSecrets.ethereumKeyPair()?.privateKey?.toHexString(withPrefix = true)
            ?: error("No seed found for account ${metaAccount.name} in ${chain.name}")
    }

    suspend fun getAccountSeed(metaId: Long): String = withContext(Dispatchers.Default) {
        val accountSecrets = secretStoreV2.getMetaAccountSecrets(metaId)

        accountSecrets?.seed?.toHexString(withPrefix = true)
            ?: error("No seed found for account $metaId")
    }

    suspend fun getEthereumPrivateKey(metaId: Long): String = withContext(Dispatchers.Default) {
        val accountSecrets = secretStoreV2.getMetaAccountSecrets(metaId)

        accountSecrets?.ethereumKeypair?.privateKey?.toHexString(withPrefix = true)
            ?: error("No seed found for account $metaId")
    }
}

suspend fun ExportPrivateKeyInteractor.getAccountSeedOrNull(metaId: Long, chainId: ChainId): String? {
    return runCatching { getAccountSeed(metaId, chainId) }.getOrNull()
}

suspend fun ExportPrivateKeyInteractor.getEthereumPrivateKeyOrNull(metaId: Long, chainId: ChainId): String? {
    return runCatching { getEthereumPrivateKey(metaId, chainId) }.getOrNull()
}

suspend fun ExportPrivateKeyInteractor.getAccountSeedOrNull(metaId: Long): String? {
    return runCatching { getAccountSeed(metaId) }.getOrNull()
}

suspend fun ExportPrivateKeyInteractor.getEthereumPrivateKeyOrNull(metaId: Long): String? {
    return runCatching { getEthereumPrivateKey(metaId) }.getOrNull()
}
