package io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets

import android.database.sqlite.SQLiteConstraintException
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_account_api.data.proxy.ProxySyncService
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.BaseAddAccountRepository
import io.novafoundation.nova.feature_account_api.domain.account.advancedEncryption.AdvancedEncryption
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountAlreadyExistsException
import io.novafoundation.nova.feature_account_api.domain.model.AddAccountType
import io.novafoundation.nova.feature_account_impl.data.repository.datasource.AccountDataSource
import io.novafoundation.nova.feature_account_impl.data.secrets.AccountSecretsFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class SecretsAddAccountRepository<T>(
    private val accountDataSource: AccountDataSource,
    private val accountSecretsFactory: AccountSecretsFactory,
    private val chainRegistry: ChainRegistry,
    private val proxySyncService: ProxySyncService
) : BaseAddAccountRepository<T>(proxySyncService) {

    protected suspend fun pickCryptoType(addAccountType: AddAccountType, advancedEncryption: AdvancedEncryption): CryptoType {
        val cryptoType = if (addAccountType is AddAccountType.ChainAccount && chainRegistry.getChain(addAccountType.chainId).isEthereumBased) {
            advancedEncryption.ethereumCryptoType
        } else {
            advancedEncryption.substrateCryptoType
        }

        requireNotNull(cryptoType) { "Expected crypto type was null" }

        return cryptoType
    }

    protected suspend fun addSecretsAccount(
        derivationPaths: AdvancedEncryption.DerivationPaths,
        addAccountType: AddAccountType,
        accountSource: AccountSecretsFactory.AccountSource
    ): Long = withContext(Dispatchers.Default) {
        when (addAccountType) {
            is AddAccountType.MetaAccount -> {
                addMetaAccount(derivationPaths, accountSource, addAccountType)
            }

            is AddAccountType.ChainAccount -> {
                addChainAccount(addAccountType, derivationPaths, accountSource)
            }
        }
    }

    private suspend fun addMetaAccount(
        derivationPaths: AdvancedEncryption.DerivationPaths,
        accountSource: AccountSecretsFactory.AccountSource,
        addAccountType: AddAccountType.MetaAccount
    ): Long {
        val (secrets, substrateCryptoType) = accountSecretsFactory.metaAccountSecrets(
            substrateDerivationPath = derivationPaths.substrate,
            ethereumDerivationPath = derivationPaths.ethereum,
            accountSource = accountSource
        )

        return transformingInsertionErrors {
            @Suppress("DEPRECATION")
            accountDataSource.insertMetaAccountFromSecrets(
                name = addAccountType.name,
                substrateCryptoType = substrateCryptoType,
                secrets = secrets
            )
        }
    }

    private suspend fun addChainAccount(
        addAccountType: AddAccountType.ChainAccount,
        derivationPaths: AdvancedEncryption.DerivationPaths,
        accountSource: AccountSecretsFactory.AccountSource
    ): Long {
        val chain = chainRegistry.getChain(addAccountType.chainId)

        val derivationPath = if (chain.isEthereumBased) derivationPaths.ethereum else derivationPaths.substrate

        val (secrets, cryptoType) = accountSecretsFactory.chainAccountSecrets(
            derivationPath = derivationPath,
            accountSource = accountSource,
            isEthereum = chain.isEthereumBased
        )

        transformingInsertionErrors {
            @Suppress("DEPRECATION")
            accountDataSource.insertChainAccount(
                metaId = addAccountType.metaId,
                chain = chain,
                cryptoType = cryptoType,
                secrets = secrets
            )
        }

        return addAccountType.metaId
    }

    private inline fun <R> transformingInsertionErrors(action: () -> R) = try {
        action()
    } catch (_: SQLiteConstraintException) {
        throw AccountAlreadyExistsException()
    }
}
