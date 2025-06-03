package io.novafoundation.nova.feature_account_migration.domain

import io.novafoundation.nova.feature_account_api.data.repository.addAccount.AddAccountResult
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.secrets.MnemonicAddAccountRepository
import io.novafoundation.nova.feature_account_api.domain.account.advancedEncryption.AdvancedEncryption
import io.novafoundation.nova.feature_account_api.domain.account.common.EncryptionDefaults
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.AddAccountType
import io.novasama.substrate_sdk_android.encrypt.mnemonic.MnemonicCreator

class AccountMigrationInteractor(
    private val addAccountRepository: MnemonicAddAccountRepository,
    private val encryptionDefaults: EncryptionDefaults,
    private val accountRepository: AccountRepository
) {

    suspend fun isPinCodeSet(): Boolean = accountRepository.isCodeSet()

    suspend fun addAccount(name: String, entropy: ByteArray) {
        val mnemonic = MnemonicCreator.fromEntropy(entropy)

        val advancedEncryption = AdvancedEncryption(
            substrateCryptoType = encryptionDefaults.substrateCryptoType,
            ethereumCryptoType = encryptionDefaults.ethereumCryptoType,
            derivationPaths = AdvancedEncryption.DerivationPaths(
                substrate = encryptionDefaults.substrateDerivationPath,
                ethereum = encryptionDefaults.ethereumDerivationPath
            )
        )

        val payload = MnemonicAddAccountRepository.Payload(
            mnemonic.words,
            advancedEncryption,
            AddAccountType.MetaAccount(name)
        )

        val addAccountResult = addAccountRepository.addAccount(payload)

        require(addAccountResult is AddAccountResult.AccountAdded)

        accountRepository.selectMetaAccount(addAccountResult.metaId)
    }
}
