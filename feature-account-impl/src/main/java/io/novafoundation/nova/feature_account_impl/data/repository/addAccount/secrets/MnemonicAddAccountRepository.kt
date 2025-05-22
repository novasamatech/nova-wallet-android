package io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets

import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountResult
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.secrets.MnemonicAddAccountRepository
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.secrets.MnemonicAddAccountRepository.*
import io.novafoundation.nova.feature_account_api.domain.account.advancedEncryption.AdvancedEncryption
import io.novafoundation.nova.feature_account_api.domain.model.AddAccountType
import io.novafoundation.nova.feature_account_impl.data.repository.datasource.AccountDataSource
import io.novafoundation.nova.feature_account_impl.data.secrets.AccountSecretsFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

class RealMnemonicAddAccountRepository(
    accountDataSource: AccountDataSource,
    accountSecretsFactory: AccountSecretsFactory,
    chainRegistry: ChainRegistry,
    metaAccountChangesEventBus: MetaAccountChangesEventBus
) : SecretsAddAccountRepository<Payload>(
    accountDataSource,
    accountSecretsFactory,
    chainRegistry,
    metaAccountChangesEventBus
), MnemonicAddAccountRepository {

    override suspend fun addAccountInternal(payload: Payload): AddAccountResult {
        return addSecretsAccount(
            derivationPaths = payload.advancedEncryption.derivationPaths,
            addAccountType = payload.addAccountType,
            accountSource = AccountSecretsFactory.AccountSource.Mnemonic(
                cryptoType = pickCryptoType(payload.addAccountType, payload.advancedEncryption),
                mnemonic = payload.mnemonic
            )
        )
    }
}
