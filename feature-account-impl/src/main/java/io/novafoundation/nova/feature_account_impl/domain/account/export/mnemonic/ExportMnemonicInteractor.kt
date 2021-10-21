package io.novafoundation.nova.feature_account_impl.domain.account.export.mnemonic

import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.secrets.v2.entropy
import io.novafoundation.nova.feature_account_api.data.secrets.derivationPath
import io.novafoundation.nova.feature_account_api.data.secrets.getAccountSecrets
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_impl.domain.account.export.ExportingSecret
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.encrypt.mnemonic.Mnemonic
import jp.co.soramitsu.fearless_utils.encrypt.mnemonic.MnemonicCreator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExportMnemonicInteractor(
    private val accountRepository: AccountRepository,
    private val secretStoreV2: SecretStoreV2,
    private val chainRegistry: ChainRegistry,
) {

    suspend fun getMnemonic(
        metaId: Long,
        chainId: ChainId,
    ): ExportingSecret<Mnemonic> = withContext(Dispatchers.Default) {
        val metaAccount = accountRepository.getMetaAccount(metaId)
        val chain = chainRegistry.getChain(chainId)

        val accountSecrets = secretStoreV2.getAccountSecrets(metaAccount, chain)

        val entropy = secretStoreV2.getAccountSecrets(metaAccount, chain).entropy()
            ?: error("No mnemonic found for account ${metaAccount.name} in ${chain.name}")

        ExportingSecret(
            derivationPath = accountSecrets.derivationPath(chain),
            secret = MnemonicCreator.fromEntropy(entropy)
        )
    }
}
