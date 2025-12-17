package io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets

import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountResult
import io.novafoundation.nova.feature_account_api.domain.model.AddAccountType
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.BaseAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets.TrustWalletAddAccountRepository.Payload
import io.novafoundation.nova.feature_account_impl.data.repository.datasource.AccountDataSource
import io.novafoundation.nova.feature_account_impl.data.repository.datasource.migration.model.ChainAccountInsertionData
import io.novafoundation.nova.feature_account_impl.data.repository.datasource.migration.model.MetaAccountInsertionData
import io.novafoundation.nova.feature_account_impl.data.secrets.TrustWalletSecretsFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import javax.inject.Inject

@FeatureScope
class TrustWalletAddAccountRepository @Inject constructor(
    val accountDataSource: AccountDataSource,
    val accountSecretsFactory: TrustWalletSecretsFactory,
    val chainRegistry: ChainRegistry,
    metaAccountChangesEventBus: MetaAccountChangesEventBus
) : BaseAddAccountRepository<Payload>(metaAccountChangesEventBus) {

    class Payload(
        val mnemonic: String,
        val addAccountType: AddAccountType.MetaAccount
    )

    override suspend fun addAccountInternal(payload: Payload): AddAccountResult {
        val (secrets, chainAccountSecrets, substrateCryptoType) = accountSecretsFactory.metaAccountSecrets(payload.mnemonic)

        val metaId = transformingAccountInsertionErrors {
            accountDataSource.insertMetaAccountWithChainAccounts(
                MetaAccountInsertionData(
                    name = payload.addAccountType.name,
                    substrateCryptoType = substrateCryptoType,
                    secrets = secrets
                ),
                chainAccountSecrets.map { (chainId, derivationPath) ->
                    ChainAccountInsertionData(
                        chain = chainRegistry.getChain(chainId),
                        cryptoType = substrateCryptoType,
                        secrets = derivationPath
                    )
                }
            )
        }

        return AddAccountResult.AccountAdded(metaId, LightMetaAccount.Type.SECRETS)
    }
}
