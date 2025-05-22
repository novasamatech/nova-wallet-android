package io.novafoundation.nova.feature_account_migration.di

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.secrets.MnemonicAddAccountRepository
import io.novafoundation.nova.feature_account_api.domain.account.common.EncryptionDefaults
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository

interface AccountMigrationFeatureDependencies {

    val resourceManager: ResourceManager

    val mnemonicAddAccountRepository: MnemonicAddAccountRepository

    val encryptionDefaults: EncryptionDefaults

    val accountRepository: AccountRepository
}
