package jp.co.soramitsu.feature_account_impl.domain.account.export.seed

import jp.co.soramitsu.common.data.secrets.v2.SecretStoreV2
import jp.co.soramitsu.common.data.secrets.v2.seed
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.feature_account_api.data.secrets.derivationPath
import jp.co.soramitsu.feature_account_api.data.secrets.getAccountSecrets
import jp.co.soramitsu.feature_account_api.domain.interfaces.AccountRepository
import jp.co.soramitsu.feature_account_impl.domain.account.export.ExportingSecret
import jp.co.soramitsu.runtime.multiNetwork.ChainRegistry
import jp.co.soramitsu.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExportSeedInteractor(
    private val accountRepository: AccountRepository,
    private val secretStoreV2: SecretStoreV2,
    private val chainRegistry: ChainRegistry,
) {

    suspend fun getSeedForExport(
        metaId: Long,
        chainId: ChainId,
    ): ExportingSecret<String> = withContext(Dispatchers.Default) {
        val metaAccount = accountRepository.getMetaAccount(metaId)
        val chain = chainRegistry.getChain(chainId)

        val accountSecrets = secretStoreV2.getAccountSecrets(metaAccount, chain)

        ExportingSecret(
            derivationPath = accountSecrets.derivationPath(chain),
            secret = accountSecrets.seed()?.toHexString(withPrefix = true)
                ?: error("No seed found for account ${metaAccount.name} in ${chain.name}")
        )
    }
}
