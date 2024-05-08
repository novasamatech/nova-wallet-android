package io.novafoundation.nova.feature_account_impl.domain.account.export.mnemonic

import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.secrets.v2.entropy
import io.novafoundation.nova.feature_account_api.data.secrets.getAccountSecrets
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.encrypt.mnemonic.Mnemonic
import io.novasama.substrate_sdk_android.encrypt.mnemonic.MnemonicCreator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.web3j.abi.datatypes.Bool

class ExportMnemonicInteractor(
    private val accountRepository: AccountRepository,
    private val secretStoreV2: SecretStoreV2,
    private val chainRegistry: ChainRegistry,
) {

    suspend fun getMnemonic(
        metaId: Long,
        chainId: ChainId,
    ): Mnemonic = withContext(Dispatchers.Default) {
        val metaAccount = accountRepository.getMetaAccount(metaId)
        val chain = chainRegistry.getChain(chainId)

        val entropy = secretStoreV2.getAccountSecrets(metaAccount, chain).entropy()
            ?: error("No mnemonic found for account ${metaAccount.name} in ${chain.name}")

        MnemonicCreator.fromEntropy(entropy)
    }

    suspend fun getMnemonic(metaId: Long): Mnemonic = withContext(Dispatchers.Default) {
        val entropy = secretStoreV2.getMetaAccountSecrets(metaId)?.entropy
            ?: error("No mnemonic found for account $metaId")

        MnemonicCreator.fromEntropy(entropy)
    }
}

suspend fun ExportMnemonicInteractor.getMnemonicOrNull(metaId: Long, chainId: ChainId?): Mnemonic? {
    return runCatching {
        if (chainId == null) {
            getMnemonic(metaId)
        } else {
            getMnemonic(metaId, chainId)
        }
    }.getOrNull()
}

suspend fun ExportMnemonicInteractor.hasMnemonic(metaId: Long, chainId: ChainId?): Boolean {
    return runCatching {
        if (chainId == null) {
            getMnemonic(metaId)
        } else {
            getMnemonic(metaId, chainId)
        }
    }.getOrNull() != null
}
