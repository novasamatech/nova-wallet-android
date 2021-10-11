package jp.co.soramitsu.feature_account_impl.domain.account.add

import jp.co.soramitsu.core.model.CryptoType
import jp.co.soramitsu.feature_account_api.domain.interfaces.AccountRepository
import jp.co.soramitsu.feature_account_api.domain.model.AddAccountType
import jp.co.soramitsu.feature_account_api.domain.model.ImportJsonMetaData
import jp.co.soramitsu.feature_account_impl.data.repository.AddAccountRepository

class AddAccountInteractor(
    private val addAccountRepository: AddAccountRepository,
    private val accountRepository: AccountRepository,
) {

    suspend fun createAccount(
        accountName: String,
        mnemonic: String,
        encryptionType: CryptoType,
        derivationPath: String,
        addAccountType: AddAccountType
    ): Result<Unit> {
        return addAccount(addAccountType) {
            addAccountRepository.addFromMnemonic(
                accountName,
                mnemonic,
                encryptionType,
                derivationPath,
                addAccountType
            )
        }
    }

    suspend fun importFromMnemonic(
        accountName: String,
        mnemonic: String,
        encryptionType: CryptoType,
        derivationPath: String,
        addAccountType: AddAccountType
    ): Result<Unit> {
        return addAccount(addAccountType) {
            addAccountRepository.addFromMnemonic(
                accountName,
                mnemonic,
                encryptionType,
                derivationPath,
                addAccountType
            )
        }
    }

    suspend fun importFromSeed(
        accountName: String,
        seed: String,
        encryptionType: CryptoType,
        derivationPath: String,
        addAccountType: AddAccountType
    ): Result<Unit> {
        return addAccount(addAccountType) {
            addAccountRepository.addFromSeed(
                accountName,
                seed,
                encryptionType,
                derivationPath,
                addAccountType
            )
        }
    }

    suspend fun importFromJson(
        json: String,
        password: String,
        name: String,
        addAccountType: AddAccountType
    ): Result<Unit> {
        return addAccount(addAccountType) {
            addAccountRepository.addFromJson(
                accountName = name,
                json = json,
                password = password,
                derivationPath = "", // TODO consider allowing derivation path for json importing,
                addAccountType = addAccountType
            )
        }
    }

    private suspend inline fun addAccount(addAccountType: AddAccountType, accountInserter: () -> Long) = runCatching {
        val metaId = accountInserter()

        if (addAccountType is AddAccountType.MetaAccount) {
            accountRepository.selectMetaAccount(metaId)
        }
    }

    suspend fun extractJsonMetadata(json: String): Result<ImportJsonMetaData> {
        return runCatching {
            addAccountRepository.extractJsonMetadata(json)
        }
    }
}
