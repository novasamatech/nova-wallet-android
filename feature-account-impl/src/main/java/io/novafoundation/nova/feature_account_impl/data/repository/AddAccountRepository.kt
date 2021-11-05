package io.novafoundation.nova.feature_account_impl.data.repository

import android.database.sqlite.SQLiteConstraintException
import io.novafoundation.nova.common.data.mappers.mapEncryptionToCryptoType
import io.novafoundation.nova.common.utils.DEFAULT_DERIVATION_PATH
import io.novafoundation.nova.common.utils.removeHexPrefix
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountAlreadyExistsException
import io.novafoundation.nova.feature_account_api.domain.model.AddAccountType
import io.novafoundation.nova.feature_account_api.domain.model.ImportJsonMetaData
import io.novafoundation.nova.feature_account_impl.data.repository.datasource.AccountDataSource
import io.novafoundation.nova.feature_account_impl.data.secrets.AccountSecretsFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import jp.co.soramitsu.fearless_utils.encrypt.json.JsonSeedDecoder
import jp.co.soramitsu.fearless_utils.encrypt.junction.BIP32JunctionDecoder
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
        accountName: String,
        mnemonic: String,
        encryptionType: CryptoType,
        derivationPath: String,
        addAccountType: AddAccountType
    ): Long = withContext(Dispatchers.Default) {
        addAccount(
            accountName = accountName,
            derivationPath = derivationPath,
            addAccountType = addAccountType,
            accountSource = AccountSecretsFactory.AccountSource.Mnemonic(encryptionType, mnemonic)
        )
    }

    suspend fun addFromSeed(
        accountName: String,
        seed: String,
        encryptionType: CryptoType,
        derivationPath: String,
        addAccountType: AddAccountType
    ): Long = withContext(Dispatchers.Default) {

        addAccount(
            accountName = accountName,
            derivationPath = derivationPath,
            addAccountType = addAccountType,
            accountSource = AccountSecretsFactory.AccountSource.Seed(encryptionType, seed)
        )
    }

    suspend fun addFromJson(
        accountName: String,
        json: String,
        password: String,
        derivationPath: String,
        addAccountType: AddAccountType
    ): Long = withContext(Dispatchers.Default) {

        addAccount(
            accountName = accountName,
            derivationPath = derivationPath,
            addAccountType = addAccountType,
            accountSource = AccountSecretsFactory.AccountSource.Json(json, password)
        )
    }

    /**
     * @return id of inserted/modified metaAccount
     */
    private suspend fun addAccount(
        accountName: String,
        derivationPath: String,
        addAccountType: AddAccountType,
        accountSource: AccountSecretsFactory.AccountSource
    ): Long {
        return when (addAccountType) {
            is AddAccountType.MetaAccount -> {
                val (secrets, substrateCryptoType) = accountSecretsFactory.metaAccountSecrets(
                    substrateDerivationPath = derivationPath,
                    accountSource = accountSource
                )

                transformingInsertionErrors {
                    accountDataSource.insertMetaAccount(
                        name = accountName,
                        substrateCryptoType = substrateCryptoType,
                        secrets = secrets
                    )
                }
            }

            is AddAccountType.ChainAccount -> {
                val chain = chainRegistry.getChain(addAccountType.chainId)

                val derivationPathOrDefault = if (derivationPath.isEmpty() && chain.isEthereumBased) {
                    BIP32JunctionDecoder.DEFAULT_DERIVATION_PATH
                } else {
                    derivationPath
                }

                val (secrets, cryptoType) = accountSecretsFactory.chainAccountSecrets(
                    derivationPath = derivationPathOrDefault,
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
