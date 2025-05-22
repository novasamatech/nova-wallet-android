package io.novafoundation.nova.feature_account_migration.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.secrets.MnemonicAddAccountRepository
import io.novafoundation.nova.feature_account_api.domain.account.common.EncryptionDefaults
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_migration.domain.AccountMigrationInteractor
import io.novafoundation.nova.feature_account_migration.utils.AccountMigrationMixinProvider
import io.novafoundation.nova.feature_account_migration.utils.common.KeyExchangeUtils

@Module
class AccountMigrationFeatureModule {

    @Provides
    @FeatureScope
    fun provideKeyExchangeUtils(): KeyExchangeUtils {
        return KeyExchangeUtils()
    }

    @Provides
    @FeatureScope
    fun provideExchangeSecretsMixinProvider(
        keyExchangeUtils: KeyExchangeUtils
    ): AccountMigrationMixinProvider {
        return AccountMigrationMixinProvider(keyExchangeUtils)
    }

    @Provides
    @FeatureScope
    fun provideAccountMigrationInteractor(
        addAccountRepository: MnemonicAddAccountRepository,
        encryptionDefaults: EncryptionDefaults,
        accountRepository: AccountRepository
    ): AccountMigrationInteractor {
        return AccountMigrationInteractor(addAccountRepository, encryptionDefaults, accountRepository)
    }
}
