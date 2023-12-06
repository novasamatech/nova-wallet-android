package io.novafoundation.nova.feature_account_impl.data.repository

import android.database.sqlite.SQLiteConstraintException
import io.novafoundation.nova.common.data.mappers.mapEncryptionToCryptoType
import io.novafoundation.nova.common.utils.removeHexPrefix
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountAlreadyExistsException
import io.novafoundation.nova.feature_account_api.domain.model.AddAccountType
import io.novafoundation.nova.feature_account_api.domain.model.ImportJsonMetaData
import io.novafoundation.nova.feature_account_impl.data.repository.datasource.AccountDataSource
import io.novafoundation.nova.feature_account_impl.data.secrets.AccountSecretsFactory
import io.novafoundation.nova.feature_account_api.domain.account.advancedEncryption.AdvancedEncryption
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import jp.co.soramitsu.fearless_utils.encrypt.json.JsonSeedDecoder
import jp.co.soramitsu.fearless_utils.encrypt.model.NetworkTypeIdentifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AddAccountRepository(
    private val accountDataSource: AccountDataSource,
    private val accountSecretsFactory: AccountSecretsFactory,
    private val jsonSeedDecoder: JsonSeedDecoder,
    private val chainRegistry: ChainRegistry,
) {

    suspend fun addFromMnemonic(
        mnemonic: String,
        advancedEncryption: AdvancedEncryption,
        addAccountType: AddAccountType
    ): Long = withContext(Dispatchers.Default) {
        addAccount(
            derivationPaths = advancedEncryption.derivationPaths,
            addAccountType = addAccountType,
            accountSource = AccountSecretsFactory.AccountSource.Mnemonic(
                cryptoType = pickCryptoType(addAccountType, advancedEncryption),
                mnemonic = mnemonic
            )
        )
    }

    suspend fun addFromSeed(
        seed: String,
        advancedEncryption: AdvancedEncryption,
        addAccountType: AddAccountType
    ): Long = withContext(Dispatchers.Default) {
        addAccount(
            derivationPaths = advancedEncryption.derivationPaths,
            addAccountType = addAccountType,
            accountSource = AccountSecretsFactory.AccountSource.Seed(
                cryptoType = pickCryptoType(addAccountType, advancedEncryption),
                seed = seed
            )
        )
    }

    suspend fun addFromJson(
        json: String,
        password: String,
        addAccountType: AddAccountType
    ): Long = withContext(Dispatchers.Default) {
        addAccount(
            derivationPaths = AdvancedEncryption.DerivationPaths.empty(),
            addAccountType = addAccountType,
            accountSource = AccountSecretsFactory.AccountSource.Json(json, password)
        )
    }

    private suspend fun pickCryptoType(addAccountType: AddAccountType, advancedEncryption: AdvancedEncryption): CryptoType {
        val cryptoType = if (addAccountType is AddAccountType.ChainAccount && chainRegistry.getChain(addAccountType.chainId).isEthereumBased) {
            advancedEncryption.ethereumCryptoType
        } else {
            advancedEncryption.substrateCryptoType
        }

        requireNotNull(cryptoType) { "Expected crypto type was null" }

        return cryptoType
    }

    /**
     * @return id of inserted/modified metaAccount
     */
    private suspend fun addAccount(
        derivationPaths: AdvancedEncryption.DerivationPaths,
        addAccountType: AddAccountType,
        accountSource: AccountSecretsFactory.AccountSource
    ): Long {
        return when (addAccountType) {
            is AddAccountType.MetaAccount -> {
                val (secrets, substrateCryptoType) = accountSecretsFactory.metaAccountSecrets(
                    substrateDerivationPath = derivationPaths.substrate,
                    ethereumDerivationPath = derivationPaths.ethereum,
                    accountSource = accountSource
                )

                transformingInsertionErrors {
                    accountDataSource.insertMetaAccountFromSecrets(
                        name = addAccountType.name,
                        substrateCryptoType = substrateCryptoType,
                        secrets = secrets
                    )
                }
            }

            is AddAccountType.ChainAccount -> {
                val chain = chainRegistry.getChain(addAccountType.chainId)

                val derivationPath = if (chain.isEthereumBased) derivationPaths.ethereum else derivationPaths.substrate

                val (secrets, cryptoType) = accountSecretsFactory.chainAccountSecrets(
                    derivationPath = derivationPath,
                    accountSource = accountSource,
                    isEthereum = chain.isEthereumBased
                )

                transformingInsertionErrors {
                    accountDataSource.insertChainAccount(
                        metaId = addAccountType.metaId,
                        chain = chain,
                        cryptoType = cryptoType,
                        secrets = secrets
                    )
                }

                addAccountType.metaId
            }
        }
    }

    suspend fun extractJsonMetadata(importJson: String): ImportJsonMetaData = withContext(Dispatchers.Default) {
        val importAccountMeta = jsonSeedDecoder.extractImportMetaData(importJson)

        with(importAccountMeta) {
            val chainId = (networkTypeIdentifier as? NetworkTypeIdentifier.Genesis)?.genesis?.removeHexPrefix()
            val cryptoType = mapEncryptionToCryptoType(encryption.encryptionType)

            ImportJsonMetaData(name, chainId, cryptoType)
        }
    }

    private inline fun <R> transformingInsertionErrors(action: () -> R) = try {
        action()
    } catch (_: SQLiteConstraintException) {
        throw AccountAlreadyExistsException()
    }
}
