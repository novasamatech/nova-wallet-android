package io.novafoundation.nova.feature_account_impl.domain.account.add

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.AddAccountType
import io.novafoundation.nova.feature_account_api.domain.model.ImportJsonMetaData
import io.novafoundation.nova.feature_account_api.domain.account.advancedEncryption.AdvancedEncryption
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets.JsonAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets.MnemonicAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets.SeedAddAccountRepository

class AddAccountInteractor(
    private val mnemonicAddAccountRepository: MnemonicAddAccountRepository,
    private val jsonAddAccountRepository: JsonAddAccountRepository,
    private val seedAddAccountRepository: SeedAddAccountRepository,
    private val accountRepository: AccountRepository,
) {

    suspend fun createAccount(
        mnemonic: String,
        advancedEncryption: AdvancedEncryption,
        addAccountType: AddAccountType
    ): Result<Unit> {
        return addAccount(addAccountType) {
            mnemonicAddAccountRepository.addAccount(
                MnemonicAddAccountRepository.Payload(
                    mnemonic,
                    advancedEncryption,
                    addAccountType
                )
            )
        }
    }

    suspend fun importFromMnemonic(
        mnemonic: String,
        advancedEncryption: AdvancedEncryption,
        addAccountType: AddAccountType
    ): Result<Unit> {
        return addAccount(addAccountType) {
            mnemonicAddAccountRepository.addAccount(
                MnemonicAddAccountRepository.Payload(
                    mnemonic,
                    advancedEncryption,
                    addAccountType
                )
            )
        }
    }

    suspend fun importFromSeed(
        seed: String,
        advancedEncryption: AdvancedEncryption,
        addAccountType: AddAccountType
    ): Result<Unit> {
        return addAccount(addAccountType) {
            seedAddAccountRepository.addAccount(
                SeedAddAccountRepository.Payload(
                    seed,
                    advancedEncryption,
                    addAccountType
                )
            )
        }
    }

    suspend fun importFromJson(
        json: String,
        password: String,
        addAccountType: AddAccountType
    ): Result<Unit> {
        return addAccount(addAccountType) {
            jsonAddAccountRepository.addAccount(
                JsonAddAccountRepository.Payload(
                    json = json,
                    password = password,
                    addAccountType = addAccountType
                )
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
            jsonAddAccountRepository.extractJsonMetadata(json)
        }
    }
}
