package io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets

import io.novafoundation.nova.feature_account_api.data.proxy.sync.ProxySyncService
import io.novafoundation.nova.feature_account_api.domain.account.advancedEncryption.AdvancedEncryption
import io.novafoundation.nova.feature_account_api.domain.model.AddAccountType
import io.novafoundation.nova.feature_account_impl.data.repository.datasource.AccountDataSource
import io.novafoundation.nova.feature_account_impl.data.secrets.AccountSecretsFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

class SeedAddAccountRepository(
    private val accountDataSource: AccountDataSource,
    private val accountSecretsFactory: AccountSecretsFactory,
    private val chainRegistry: ChainRegistry,
    private val proxySyncService: ProxySyncService,
) : SecretsAddAccountRepository<SeedAddAccountRepository.Payload>(
    accountDataSource,
    accountSecretsFactory,
    chainRegistry,
    proxySyncService
) {

    class Payload(
        val seed: String,
        val advancedEncryption: AdvancedEncryption,
        val addAccountType: AddAccountType
    )

    override suspend fun addAccountInternal(payload: Payload): Long {
        return addSecretsAccount(
            derivationPaths = payload.advancedEncryption.derivationPaths,
            addAccountType = payload.addAccountType,
            accountSource = AccountSecretsFactory.AccountSource.Seed(
                cryptoType = pickCryptoType(payload.addAccountType, payload.advancedEncryption),
                seed = payload.seed
            )
        )
    }
}
